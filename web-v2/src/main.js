import './styles.css';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { icons } from './icons.js';
import { t, setLang, getLang } from './i18n.js';
import { TrackerSocket, DEFAULT_SERVER, wsToHttp } from './protocol.js';
import {
  GPS, SessionSpeedStats, deriveSpeedKmh, getDistance, getBearing, convertSpeed, escapeHtml
} from './stats.js';
import {
  renderShell, riderRow, historyRow, toast, unitLabel, DIAL_ARC
} from './ui.js';

// ---------------- state ----------------
const LS = localStorage;
const state = {
  userId: LS.getItem('userId') || `user_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`,
  userName: LS.getItem('userName') || '',
  groupName: LS.getItem('groupName') || '',
  serverUrl: LS.getItem('serverUrl') || DEFAULT_SERVER,
  speedUnit: LS.getItem('speedUnit') || 'kmh',
  voice: LS.getItem('voiceEnabled') === 'true',
  visualizer: LS.getItem('visualizerMode') === 'true',
  autoCenter: LS.getItem('autoCenterMap') !== 'false',
  minSpeed: parseFloat(LS.getItem('minSpeedThreshold')) || 22,
  theme: LS.getItem('theme') || 'dark'
};
LS.setItem('userId', state.userId);
setLang(getLang());

let sock = null, stats = new SessionSpeedStats(), watchId = null, tracking = false;
let prevPos = null, lastPos = null, sessionTrack = [], users = [];
let map = null, markers = {}, tracks = {}, polylines = {};
let lastAnnounce = -1, lastAnnounceTime = 0;

const $ = (id) => document.getElementById(id);
const DIAL_SCALE = { kmh: 60, mph: 40, knots: 35 };

// ---------------- mount ----------------
document.documentElement.setAttribute('data-theme', state.theme);
document.getElementById('app').innerHTML = renderShell(state);
wire();
updateThemeIcon();
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
loadHistory().catch(() => {}); // warm all-time stats

// ---------------- navigation ----------------
function showScreen(name) {
  document.querySelectorAll('[data-screen]').forEach((s) => (s.hidden = s.dataset.screen !== name));
  document.querySelectorAll('[data-nav]').forEach((b) => b.classList.toggle('active', b.dataset.nav === name));
  if (name === 'settings') { openSettings(); return; }
  if (name === 'map') setTimeout(initMap, 60);
  if (name === 'board') renderLeaderboard();
  if (name === 'history') loadHistory().catch(() => {});
}

function wire() {
  document.querySelectorAll('[data-nav]').forEach((b) =>
    b.addEventListener('click', () => showScreen(b.dataset.nav)));
  $('btn-theme').addEventListener('click', toggleTheme);
  $('in-name').addEventListener('input', (e) => { state.userName = e.target.value; LS.setItem('userName', state.userName); });
  $('in-group').addEventListener('input', (e) => { state.groupName = e.target.value; LS.setItem('groupName', state.groupName); });
  $('btn-start').addEventListener('click', startTracking);
  $('btn-stop').addEventListener('click', () => stopTracking(true));
  $('btn-reset').addEventListener('click', () => { stats.reset(); sessionTrack = []; prevPos = null; renderStats(stats.update(0, 0, Date.now(), 0)); toast(t('reset'), 'info'); });
  $('btn-horn').addEventListener('click', () => { sock?.sendHorn(); playHorn(); });
  $('btn-download').addEventListener('click', downloadGpx);
  $('btn-map-clear').addEventListener('click', clearTracks);
  $('btn-map-full').addEventListener('click', () => { const el = $('map'); if (el.requestFullscreen) el.requestFullscreen(); });
}

// ---------------- tracking ----------------
function startTracking() {
  if (!state.userName.trim()) { toast(t('enterName'), 'err'); $('in-name').focus(); return; }
  if (!navigator.geolocation) { toast('GPS unavailable', 'err'); return; }
  stats.reset(); sessionTrack = []; prevPos = null; lastPos = null;
  tracking = true;
  $('config-card').hidden = true;
  $('live').hidden = false;
  $('viz-banner').hidden = !state.visualizer;
  renderStats({ current: 0, max: 0, avg: 0, avg10s: 0, max10s: 0, avg500m: 0, max500m: 0 });

  sock = new TrackerSocket({
    onConnected: () => setConn(true),
    onDisconnected: () => setConn(false),
    onUsers: (list) => { users = list; renderUsers(); if (map) updateMap(); if (!$('[data-screen=board]')?.hidden) renderLeaderboard(); },
    onHorn: (d) => { if (d.userId !== state.userId) { playHorn(); toast(t('hornReceived', { name: d.userName || '?' }), 'info'); } },
    onJump: (d) => { const name = d.userId === state.userId ? t('you') : (d.userName || '?'); toast(t('jumpAlert', { name, h: (d.maxHeight || 0).toFixed(1), t: ((d.hangtime || 0) / 1000).toFixed(1) }), 'ok', 4500); },
    onError: (m) => toast(m, 'err')
  });
  sock.connect({ userId: state.userId, userName: state.userName || 'Usuario', groupName: (state.groupName || 'default').toLowerCase(), serverUrl: state.serverUrl });

  watchId = navigator.geolocation.watchPosition(onPosition, onGeoError, {
    enableHighAccuracy: true, timeout: 15000, maximumAge: GPS.LOCATION_MAX_AGE
  });
  setGps(false);
}

function onPosition(pos) {
  const acc = pos.coords.accuracy;
  if (acc >= GPS.MAX_GPS_ACCURACY) return; // reject inaccurate fixes
  setGps(true);
  const now = Date.now();
  const dist = prevPos ? getDistance(prevPos.coords.latitude, prevPos.coords.longitude, pos.coords.latitude, pos.coords.longitude) : 0;
  const elapsed = prevPos ? (pos.timestamp || now) - (prevPos.timestamp || now) : 0;
  const rawSpeed = deriveSpeedKmh(pos.coords.speed != null, pos.coords.speed || 0, dist, elapsed);
  const current = rawSpeed < GPS.MIN_SPEED_THRESHOLD ? 0 : rawSpeed;
  const snap = stats.update(rawSpeed, current, now, dist);
  renderStats(snap);

  $('m-acc').textContent = Math.round(acc);
  $('m-coords').textContent = `${pos.coords.latitude.toFixed(5)}, ${pos.coords.longitude.toFixed(5)}`;

  sessionTrack.push({ lat: pos.coords.latitude, lon: pos.coords.longitude, speed: current, timestamp: now });
  if (sessionTrack.length >= 2) $('btn-download').disabled = false;

  let bearing = 0;
  if (prevPos && rawSpeed > 1) bearing = getBearing(prevPos.coords.latitude, prevPos.coords.longitude, pos.coords.latitude, pos.coords.longitude);

  if (!state.visualizer && sock?.isOpen) {
    sock.sendSpeed({ speed: current, maxSpeed: snap.max, lat: pos.coords.latitude, lon: pos.coords.longitude, bearing: Math.round(bearing) });
  }
  announce(current);
  prevPos = pos; lastPos = pos;
}

function onGeoError(err) { setGps(false); toast('GPS: ' + err.message, 'err'); }

async function stopTracking(save) {
  if (watchId != null) { navigator.geolocation.clearWatch(watchId); watchId = null; }
  if (save && stats.max > 0 && lastPos) await submitHistory().catch(() => {});
  sock?.close(); sock = null;
  tracking = false;
  setConn(false); setGps(false);
  const summary = { max: stats.max, max10s: stats.max10s, max500m: stats.max500m,
    distance: stats.total, duration: sessionTrack.length ? (sessionTrack[sessionTrack.length - 1].timestamp - sessionTrack[0].timestamp) : 0 };
  $('live').hidden = true;
  $('config-card').hidden = false;
  if (save && summary.max > 0) openSummary(summary);
}

// ---------------- rendering ----------------
function renderStats(s) {
  const u = state.speedUnit, cv = (v) => convertSpeed(v, u).toFixed(1);
  $('dv-speed').textContent = cv(s.current);
  const frac = Math.max(0, Math.min(1, convertSpeed(s.current, u) / DIAL_SCALE[u]));
  $('dial-fill').setAttribute('stroke-dashoffset', (DIAL_ARC * (1 - frac)).toFixed(1));
  const map2 = { 'st-avg': s.avg, 'st-max': s.max, 'st-avg10': s.avg10s, 'st-max10': s.max10s, 'st-avg500': s.avg500m, 'st-max500': s.max500m };
  for (const id in map2) $(id).textContent = cv(map2[id]);
}

function renderUsers() {
  $('live-count').textContent = users.length;
  const box = $('users-rows');
  if (!users.length) { box.innerHTML = `<div class="empty">${t('noUsers')}</div>`; return; }
  box.innerHTML = users.map((u) => riderRow(u, state.userId, state.speedUnit)).join('');
}

function renderLeaderboard() {
  const live = [...users].sort((a, b) => (b.speed || 0) - (a.speed || 0));
  const box = $('board-live');
  box.innerHTML = live.length ? live.map((u, i) => riderRow(u, state.userId, state.speedUnit, i + 1)).join('') : `<div class="empty">${t('noUsers')}</div>`;
}

function setConn(ok) {
  const p = $('pill-conn'); p.className = 'stat-dot ' + (ok ? 'ok' : 'bad');
  p.title = ok ? t('connected') : t('disconnected');
}
function setGps(ok) {
  const p = $('pill-gps'); p.className = 'stat-dot ' + (ok ? 'ok' : '');
  p.title = ok ? t('gpsActive') : t('gpsSearching');
}

// ---------------- map ----------------
function initMap() {
  if (map) { map.invalidateSize(); return; }
  map = L.map('map', { zoomControl: true, attributionControl: false }).setView([40.4, -3.7], 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
  updateMap();
}
function arrowIcon(color, bearing, name, spd) {
  return L.divIcon({ className: '', iconSize: [40, 40], iconAnchor: [20, 20], html:
    `<div style="position:relative;width:40px;height:40px">
      <div style="position:absolute;top:-30px;left:50%;transform:translateX(-50%);background:${color};color:#03121a;padding:2px 8px;border-radius:8px;font-size:11px;font-weight:800;white-space:nowrap">${escapeHtml(name)} · ${spd}</div>
      <svg width="34" height="34" viewBox="0 0 24 24" style="transform:rotate(${bearing}deg);filter:drop-shadow(0 2px 4px rgba(0,0,0,.5))"><path d="M12 2 L4 20 L12 16 L20 20 Z" fill="${color}" stroke="#0b1220" stroke-width="1.4"/></svg>
    </div>` });
}
function updateMap() {
  if (!map) return;
  users.forEach((user) => {
    if (user.lat == null || user.lon == null) return;
    const isMe = user.userId === state.userId;
    const color = isMe ? '#22d3ee' : '#34d399';
    const name = user.userName || user.userId.slice(0, 8);
    const spd = convertSpeed(user.speed || 0, state.speedUnit).toFixed(1) + ' ' + unitLabel(state.speedUnit);
    const icon = arrowIcon(color, user.bearing || 0, name, spd);
    if (markers[user.userId]) markers[user.userId].setLatLng([user.lat, user.lon]).setIcon(icon);
    else markers[user.userId] = L.marker([user.lat, user.lon], { icon }).addTo(map);
    if ((user.speed || 0) > 3.6) {
      tracks[user.userId] = tracks[user.userId] || [];
      tracks[user.userId].push([user.lat, user.lon]);
      if (tracks[user.userId].length > 50) tracks[user.userId].shift();
      if (polylines[user.userId]) map.removeLayer(polylines[user.userId]);
      if (tracks[user.userId].length > 1) polylines[user.userId] = L.polyline(tracks[user.userId], { color: isMe ? '#22d3ee' : '#818cf8', weight: 3, opacity: 0.7 }).addTo(map);
    }
    if (isMe && state.autoCenter) map.setView([user.lat, user.lon]);
  });
  Object.keys(markers).forEach((id) => { if (!users.find((u) => u.userId === id)) { map.removeLayer(markers[id]); delete markers[id]; } });
}
function clearTracks() {
  Object.values(polylines).forEach((p) => map && map.removeLayer(p));
  polylines = {}; tracks = {};
}

// ---------------- history / leaderboard data ----------------
async function submitHistory() {
  const url = `${wsToHttp(state.serverUrl)}/api/speed-history`;
  await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, keepalive: true,
    body: JSON.stringify({ userId: state.userId, userName: state.userName || 'Usuario', groupName: (state.groupName || 'default').toLowerCase(),
      maxSpeed: stats.max, maxSpeed10s: stats.max10s, maxSpeed500m: stats.max500m,
      latitude: lastPos.coords.latitude, longitude: lastPos.coords.longitude, timestamp: Date.now() }) });
}
async function loadHistory() {
  const http = wsToHttp(state.serverUrl);
  const [statsRes, listRes] = await Promise.all([
    fetch(`${http}/api/speed-history/${state.userId}/stats`).then((r) => r.json()).catch(() => null),
    fetch(`${http}/api/speed-history/${state.userId}?limit=50`).then((r) => r.json()).catch(() => null)
  ]);
  if (statsRes?.statistics) {
    $('h-high').textContent = convertSpeed(parseFloat(statsRes.statistics.highest_speed || 0), state.speedUnit).toFixed(1);
    $('h-avg').textContent = convertSpeed(parseFloat(statsRes.statistics.average_max_speed || 0), state.speedUnit).toFixed(1);
  }
  const rows = $('history-rows');
  if (rows) {
    const records = listRes?.records || [];
    rows.innerHTML = records.length ? records.map((r) => historyRow(r, state.speedUnit)).join('') : `<div class="empty">${t('noRecords')}</div>`;
  }
  // all-time leaderboard for the group (admin feed filtered by group)
  const all = await fetch(`${http}/api/speed-history?limit=200`).then((r) => r.json()).catch(() => null);
  const box = $('board-alltime');
  if (box && all?.records) {
    const grp = (state.groupName || 'default').toLowerCase();
    const best = {};
    all.records.filter((r) => (r.group_name || 'default') === grp).forEach((r) => {
      const v = parseFloat(r.max_speed) || 0;
      if (!best[r.user_id] || v > best[r.user_id].max_speed) best[r.user_id] = { userId: r.user_id, userName: r.user_name, speed: v, maxSpeed: v };
    });
    const ranked = Object.values(best).sort((a, b) => b.speed - a.speed).slice(0, 20);
    box.innerHTML = ranked.length ? ranked.map((u, i) => riderRow(u, state.userId, state.speedUnit, i + 1)).join('') : `<div class="empty">${t('noRecords')}</div>`;
  }
}

// ---------------- settings sheet ----------------
function openSettings() {
  const langs = { es: 'Español', en: 'English', fr: 'Français', de: 'Deutsch' };
  const units = { kmh: 'km/h', mph: 'mph', knots: 'knots' };
  const opt = (o, cur) => Object.entries(o).map(([v, l]) => `<option value="${v}" ${v === cur ? 'selected' : ''}>${l}</option>`).join('');
  const sw = (id, on) => `<label class="switch"><input type="checkbox" id="${id}" ${on ? 'checked' : ''}><span class="slider"></span></label>`;
  const root = $('sheet-root');
  root.innerHTML = `<div class="sheet-scrim" id="sheet-scrim"><div class="sheet" onclick="event.stopPropagation()">
    <div class="sheet-grab"></div>
    <div class="sheet-head"><h2>${t('settings')}</h2><button class="icon-btn" id="sheet-close">${icons.close}</button></div>
    <div class="field"><label class="lbl">${t('serverUrl')}</label><input class="input" id="set-server" value="${escapeHtml(state.serverUrl)}"></div>
    <div class="field"><label class="lbl">${t('language')}</label><select class="input" id="set-lang">${opt(langs, getLang())}</select></div>
    <div class="field"><label class="lbl">${t('speedUnit')}</label><select class="input" id="set-unit">${opt(units, state.speedUnit)}</select></div>
    <div class="setting-row"><span class="txt">${t('autoCenter')}</span>${sw('set-auto', state.autoCenter)}</div>
    <div class="setting-row"><span class="txt">${t('voice')}</span>${sw('set-voice', state.voice)}</div>
    <div class="setting-row"><span class="txt">${t('visualizer')}</span>${sw('set-viz', state.visualizer)}</div>
    <div class="field" style="margin-top:14px"><label class="lbl">${t('minSpeed')}</label><input class="input" type="number" id="set-min" value="${state.minSpeed}" min="0" max="200"></div>
    <p class="hint">ID: ${escapeHtml(state.userId.slice(0, 28))}</p>
  </div></div>`;
  const closeSheet = () => { root.innerHTML = ''; showScreen('track'); };
  $('sheet-scrim').addEventListener('click', closeSheet);
  $('sheet-close').addEventListener('click', closeSheet);
  $('set-server').addEventListener('change', (e) => { state.serverUrl = e.target.value.trim(); LS.setItem('serverUrl', state.serverUrl); });
  $('set-lang').addEventListener('change', (e) => { setLang(e.target.value); location.reload(); });
  $('set-unit').addEventListener('change', (e) => { state.speedUnit = e.target.value; LS.setItem('speedUnit', state.speedUnit); refreshUnits(); });
  $('set-auto').addEventListener('change', (e) => { state.autoCenter = e.target.checked; LS.setItem('autoCenterMap', state.autoCenter); });
  $('set-voice').addEventListener('change', (e) => { state.voice = e.target.checked; LS.setItem('voiceEnabled', state.voice); });
  $('set-viz').addEventListener('change', (e) => { state.visualizer = e.target.checked; LS.setItem('visualizerMode', state.visualizer); $('viz-banner').hidden = !(state.visualizer && tracking); });
  $('set-min').addEventListener('change', (e) => { state.minSpeed = parseFloat(e.target.value) || 22; LS.setItem('minSpeedThreshold', state.minSpeed); });
}

function refreshUnits() {
  document.querySelectorAll('#dv-unit,[id^=su-]').forEach((el) => (el.textContent = unitLabel(state.speedUnit)));
  if (tracking) renderStats(stats.update(0, 0, Date.now(), 0));
  renderUsers();
}

// ---------------- session summary ----------------
function openSummary(s) {
  const u = state.speedUnit, cv = (v) => convertSpeed(v, u).toFixed(1);
  const mins = Math.floor(s.duration / 60000), secs = Math.floor((s.duration % 60000) / 1000);
  const root = $('sheet-root');
  root.innerHTML = `<div class="sheet-scrim" id="sum-scrim"><div class="sheet" onclick="event.stopPropagation()">
    <div class="sheet-grab"></div>
    <div class="sheet-head"><h2>${t('sessionDone')}</h2><button class="icon-btn" id="sum-close">${icons.close}</button></div>
    <div class="summary-hero"><div class="big tabular">${cv(s.max)}</div><div class="lbl">${t('topSpeed')} · ${unitLabel(u)}</div></div>
    <div class="stat-grid" style="margin-top:16px">
      <div class="stat p"><div class="k">${t('max10s')}</div><div class="v tabular">${cv(s.max10s)}</div><div class="u">${unitLabel(u)}</div></div>
      <div class="stat i"><div class="k">${t('max500m')}</div><div class="v tabular">${cv(s.max500m)}</div><div class="u">${unitLabel(u)}</div></div>
      <div class="stat b"><div class="k">${t('distance')}</div><div class="v tabular">${(s.distance / 1000).toFixed(2)}</div><div class="u">km</div></div>
      <div class="stat g"><div class="k">${t('duration')}</div><div class="v tabular">${mins}:${String(secs).padStart(2, '0')}</div><div class="u">min</div></div>
    </div>
    <button class="btn btn-primary" id="sum-ok" style="margin-top:18px">${t('close')}</button>
  </div></div>`;
  const close = () => { root.innerHTML = ''; loadHistory().catch(() => {}); };
  $('sum-scrim').addEventListener('click', close);
  $('sum-close').addEventListener('click', close);
  $('sum-ok').addEventListener('click', close);
}

// ---------------- misc ----------------
function announce(speed) {
  if (!state.voice || !window.speechSynthesis) return;
  const conv = convertSpeed(speed, state.speedUnit);
  if (conv < state.minSpeed) return;
  const now = Date.now(), v = Math.round(conv);
  if (v !== lastAnnounce && now - lastAnnounceTime > 3000) {
    window.speechSynthesis.speak(new SpeechSynthesisUtterance(String(v)));
    lastAnnounce = v; lastAnnounceTime = now;
  }
}
function playHorn() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    [220, 330].forEach((f) => {
      const o = ctx.createOscillator(), g = ctx.createGain();
      o.type = 'sawtooth'; o.frequency.value = f; o.connect(g); g.connect(ctx.destination);
      g.gain.setValueAtTime(0.15, ctx.currentTime); g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.4);
      o.start(); o.stop(ctx.currentTime + 1.4);
    });
  } catch {}
}
function downloadGpx() {
  if (sessionTrack.length < 2) return;
  const pts = sessionTrack.map((p) => `<trkpt lat="${p.lat}" lon="${p.lon}"><time>${new Date(p.timestamp).toISOString()}</time></trkpt>`).join('');
  const gpx = `<?xml version="1.0"?><gpx version="1.1" creator="3WB Tracker"><trk><name>3WB ${new Date().toISOString()}</name><trkseg>${pts}</trkseg></trk></gpx>`;
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([gpx], { type: 'application/gpx+xml' }));
  a.download = `3wb-${Date.now()}.gpx`; a.click();
}
function toggleTheme() {
  state.theme = state.theme === 'dark' ? 'light' : 'dark';
  LS.setItem('theme', state.theme);
  document.documentElement.setAttribute('data-theme', state.theme);
  updateThemeIcon();
}
function updateThemeIcon() { $('btn-theme').innerHTML = state.theme === 'dark' ? icons.moon : icons.sun; }
