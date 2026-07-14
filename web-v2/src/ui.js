// Pure view builders for v2.0. Return HTML strings; main.js wires behavior.
import { icons } from './icons.js';
import { t } from './i18n.js';
import { escapeHtml, convertSpeed } from './stats.js';

export const DIAL_R = 112;
export const DIAL_C = 2 * Math.PI * DIAL_R; // circumference
export const DIAL_ARC = DIAL_C * 0.75; // 270° visible sweep

export function unitLabel(u) { return u === 'kmh' ? 'km/h' : u; }

function statCell(cls, key, id, unitId, u) {
  return `<div class="stat ${cls}">
    <div class="k">${t(key)}</div>
    <div class="v tabular" id="${id}">0.0</div>
    <div class="u" id="${unitId}">${unitLabel(u)}</div>
  </div>`;
}

export function renderShell(s) {
  const u = s.speedUnit;
  return `
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="mark">${icons.gauge}</span>
        <h1>3WB Tracker</h1>
      </div>
      <span class="stat-dot" id="pill-conn" title="${t('disconnected')}">${icons.wifi}</span>
      <span class="stat-dot" id="pill-gps" title="GPS">${icons.sat}</span>
      <button class="icon-btn" id="btn-theme" aria-label="theme">${icons.moon}</button>
    </header>

    <main class="screens">
      ${screenTrack(s, u)}
      ${screenMap()}
      ${screenBoard()}
      ${screenHistory()}
    </main>

    <nav class="bottom-nav">
      ${navBtn('track', icons.gauge, t('track'), true)}
      ${navBtn('map', icons.map, t('map'))}
      ${navBtn('board', icons.trophy, t('board'))}
      ${navBtn('history', icons.history, t('history'))}
      ${navBtn('settings', icons.settings, t('settings'))}
    </nav>
  </div>
  <div class="toast-wrap" id="toasts"></div>
  <div id="sheet-root"></div>`;
}

function navBtn(id, icon, label, active) {
  return `<button class="nav-btn ${active ? 'active' : ''}" data-nav="${id}">${icon}<span>${label}</span></button>`;
}

function screenTrack(s, u) {
  return `<section class="screen" data-screen="track">
    <div id="config-card" class="card">
      <div class="field">
        <label class="lbl">${t('yourName')}</label>
        <input class="input" id="in-name" maxlength="20" placeholder="${t('namePh')}" value="${escapeHtml(s.userName)}" />
        <p class="hint">${t('nameHelper')}</p>
      </div>
      <div class="field">
        <label class="lbl">${t('groupName')}</label>
        <input class="input" id="in-group" maxlength="30" placeholder="${t('groupPh')}" value="${escapeHtml(s.groupName)}" />
        <p class="hint">${t('groupHelper')}</p>
      </div>
      <button class="btn btn-primary" id="btn-start">${icons.play}<span>${t('start')}</span></button>
    </div>

    <div id="live" hidden>
      <div id="viz-banner" class="banner" hidden>${icons.info}<span>${t('visualizerActive')}</span></div>

      <div class="dial-wrap">
        <div class="dial">
          <svg viewBox="0 0 260 260" aria-hidden="true">
            <defs><linearGradient id="dialGrad" x1="0" y1="1" x2="1" y2="0">
              <stop offset="0" stop-color="#22d3ee"/><stop offset="1" stop-color="#38bdf8"/>
            </linearGradient></defs>
            <circle class="track" cx="130" cy="130" r="${DIAL_R}"
              stroke-dasharray="${DIAL_ARC.toFixed(1)} ${DIAL_C.toFixed(1)}"></circle>
            <circle class="fill" id="dial-fill" cx="130" cy="130" r="${DIAL_R}"
              stroke-dasharray="${DIAL_ARC.toFixed(1)} ${DIAL_C.toFixed(1)}"
              stroke-dashoffset="${DIAL_ARC.toFixed(1)}"></circle>
          </svg>
          <div class="dial-value">
            <div class="big tabular" id="dv-speed">0.0</div>
            <div class="unit" id="dv-unit">${unitLabel(u)}</div>
            <div class="lbl">${t('currentSpeed')}</div>
          </div>
        </div>
      </div>

      <div class="stat-grid">
        ${statCell('b', 'avg', 'st-avg', 'su-avg', u)}
        ${statCell('g', 'maxSession', 'st-max', 'su-max', u)}
        ${statCell('p', 'avg10s', 'st-avg10', 'su-avg10', u)}
        ${statCell('p', 'max10s', 'st-max10', 'su-max10', u)}
        ${statCell('i', 'avg500m', 'st-avg500', 'su-avg500', u)}
        ${statCell('i', 'max500m', 'st-max500', 'su-max500', u)}
      </div>

      <div class="gps-meta">
        <span><b>${t('accuracy')}:</b> <span id="m-acc">--</span> m</span>
        <span><b>${t('coords')}:</b> <span id="m-coords">--</span></span>
      </div>

      <div class="btn-row" style="margin-top:14px">
        <button class="btn btn-ghost" id="btn-reset">${icons.reset}<span>${t('reset')}</span></button>
        <button class="btn btn-ghost" id="btn-horn">${icons.horn}<span>${t('horn')}</span></button>
      </div>
      <button class="btn btn-ghost" id="btn-download" style="margin-top:10px" disabled>${icons.download}<span>${t('download')}</span></button>
      <button class="btn btn-danger" id="btn-stop" style="margin-top:10px">${icons.stop}<span>${t('stop')}</span></button>
    </div>

    <div class="section-title">${t('users')} · <span id="live-count">0</span></div>
    <div class="rows" id="users-rows"><div class="empty">${t('noUsers')}</div></div>
  </section>`;
}

function screenMap() {
  return `<section class="screen" data-screen="map" hidden>
    <div style="display:flex;justify-content:flex-end;gap:8px;margin:8px 0">
      <button class="icon-btn" id="btn-map-clear" title="clear">${icons.trash}</button>
      <button class="icon-btn" id="btn-map-full" title="fullscreen">${icons.expand}</button>
    </div>
    <div id="map"></div>
    <div class="legend">
      <span class="li"><span class="swatch" style="background:#22d3ee"></span>${t('yourPos')}</span>
      <span class="li"><span class="swatch" style="background:#34d399"></span>${t('others')}</span>
      <span class="li"><span class="swatch" style="background:#818cf8;height:4px"></span>${t('track_')}</span>
    </div>
  </section>`;
}

function screenBoard() {
  return `<section class="screen" data-screen="board" hidden>
    <div class="section-title">${t('liveBoard')}</div>
    <div class="rows" id="board-live"><div class="empty">${t('noUsers')}</div></div>
    <div class="section-title">${t('allTime')}</div>
    <div class="rows" id="board-alltime"><div class="empty">${t('noRecords')}</div></div>
  </section>`;
}

function screenHistory() {
  return `<section class="screen" data-screen="history" hidden>
    <div class="card">
      <div class="stat-grid" style="margin:0">
        <div class="stat b"><div class="k">${t('maxSession')}</div><div class="v tabular" id="h-high">0.0</div></div>
        <div class="stat g"><div class="k">${t('avg')}</div><div class="v tabular" id="h-avg">0.0</div></div>
      </div>
    </div>
    <div class="section-title">${t('history')}</div>
    <div class="rows" id="history-rows"><div class="empty">${t('noRecords')}</div></div>
  </section>`;
}

// ---- live render helpers ----
export function riderRow(user, meId, unit, rank) {
  const isMe = user.userId === meId;
  const name = escapeHtml(user.userName || user.userId.slice(0, 12));
  const spd = convertSpeed(user.speed || 0, unit).toFixed(1);
  const mx = convertSpeed(user.maxSpeed || 0, unit).toFixed(1);
  const initial = escapeHtml((user.userName || '?').trim().charAt(0).toUpperCase() || '?');
  const rankHtml = rank != null ? `<div class="rank ${rank <= 3 ? 'top' : ''}">${rank}</div>` : '';
  return `<div class="row ${isMe ? 'me' : ''}">
    ${rankHtml}
    <div class="avatar">${initial}</div>
    <div class="who"><div class="nm">${name} ${isMe ? `<span class="badge">${t('you')}</span>` : ''}</div>
      <div class="sub">${t('maxSession')} ${mx} ${unitLabel(unit)}</div></div>
    <div class="spd"><div class="n tabular">${spd}</div><div class="u">${unitLabel(unit)}</div></div>
  </div>`;
}

export function historyRow(rec, unit) {
  const spd = convertSpeed(parseFloat(rec.max_speed) || 0, unit).toFixed(1);
  const g = rec.group_name && rec.group_name !== 'default' ? escapeHtml(rec.group_name) : '';
  return `<div class="row">
    <div class="who"><div class="nm">${escapeHtml(rec.date)}</div>
      <div class="sub">${escapeHtml(rec.time)}${g ? ' · ' + g : ''}</div></div>
    <div class="spd"><div class="n tabular">${spd}</div><div class="u">${unitLabel(unit)}</div></div>
  </div>`;
}

export function toast(msg, kind = 'info', ms = 3200) {
  const wrap = document.getElementById('toasts');
  if (!wrap) return;
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  el.textContent = msg;
  wrap.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity .3s'; setTimeout(() => el.remove(), 300); }, ms);
}
