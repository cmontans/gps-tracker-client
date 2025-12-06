// ===== GPS TRACKER v4.0 - JAVASCRIPT COMPLETO =====

console.log('🚀 GPS Tracker v4.0 iniciado');

// ===== VARIABLES GLOBALES =====
let userId = localStorage.getItem('userId') || `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
localStorage.setItem('userId', userId);

let userName = localStorage.getItem('userName') || '';
let groupName = localStorage.getItem('groupName') || '';
let serverUrl = localStorage.getItem('serverUrl') || 'wss://gps-tracker-server-production-5900.up.railway.app';

let ws = null;
let watchId = null;
let isTracking = false;
let users = [];
let wakeLock = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 10;

let maxSpeed = 0;
let speedReadings = [];

let map = null;
let mapFullscreen = null;
let userMarkers = {};
let userTracks = {};
let trackPolylines = {};
let mapVisible = false;
let isFullscreen = false;

let keepAliveInterval = null;

// ===== ELEMENTOS DOM =====
const elements = {
  userId: document.getElementById('user-id'),
  userName: document.getElementById('user-name'),
  groupName: document.getElementById('group-name'),
  serverUrl: document.getElementById('server-url'),
  configSection: document.getElementById('config-section'),
  statusSection: document.getElementById('status-section'),
  gpsStatusContainer: document.getElementById('gps-status-container'),
  wifiIcon: document.getElementById('wifi-icon'),
  connectionStatus: document.getElementById('connection-status'),
  gpsIcon: document.getElementById('gps-icon'),
  gpsStatus: document.getElementById('gps-status'),
  speedDisplay: document.getElementById('speed-display'),
  maxSpeed: document.getElementById('max-speed'),
  avgSpeed: document.getElementById('avg-speed'),
  errorBox: document.getElementById('error-box'),
  successBox: document.getElementById('success-box'),
  gpsInfo: document.getElementById('gps-info'),
  trackButton: document.getElementById('track-button'),
  stopButton: document.getElementById('stop-button'),
  resetStatsButton: document.getElementById('reset-stats-button'),
  userCount: document.getElementById('user-count'),
  usersList: document.getElementById('users-list'),
  accuracy: document.getElementById('accuracy'),
  coords: document.getElementById('coords'),
  mapContainer: document.getElementById('map-container'),
  toggleMapButton: document.getElementById('toggle-map-button'),
  closeMapButton: document.getElementById('close-map-button'),
  clearTracksButton: document.getElementById('clear-tracks-button'),
  fullscreenButton: document.getElementById('fullscreen-button'),
  exitFullscreenButton: document.getElementById('exit-fullscreen-button'),
  mapFullscreen: document.getElementById('map-fullscreen'),
  settingsButton: document.getElementById('settings-button'),
  settingsModal: document.getElementById('settings-modal'),
  closeSettings: document.getElementById('close-settings')
};

// Inicializar valores
elements.userId.textContent = userId.substring(0, 30);
elements.userName.value = userName;
elements.groupName.value = groupName;
elements.serverUrl.value = serverUrl;

// ===== EVENT LISTENERS - INPUTS =====
elements.userName.addEventListener('input', (e) => {
  userName = e.target.value.trim();
  localStorage.setItem('userName', userName);
});

elements.groupName.addEventListener('input', (e) => {
  groupName = e.target.value.trim();
  localStorage.setItem('groupName', groupName);
});

elements.serverUrl.addEventListener('input', (e) => {
  serverUrl = e.target.value.trim();
  localStorage.setItem('serverUrl', serverUrl);
});

// ===== FUNCIONES UI =====
function showError(message) {
  console.error('❌', message);
  elements.errorBox.textContent = message;
  elements.errorBox.classList.remove('hidden');
  elements.successBox.classList.add('hidden');
  setTimeout(() => elements.errorBox.classList.add('hidden'), 8000);
}

function showSuccess(message) {
  console.log('✓', message);
  elements.successBox.textContent = message;
  elements.successBox.classList.remove('hidden');
  elements.errorBox.classList.add('hidden');
  setTimeout(() => elements.successBox.classList.add('hidden'), 3000);
}

function updateConnectionStatus(connected) {
  elements.wifiIcon.textContent = connected ? '✅' : '❌';
  elements.connectionStatus.textContent = connected ? 'Conectado' : 'Desconectado';
  elements.connectionStatus.className = `text-xs sm:text-sm font-medium ${connected ? 'text-green-600' : 'text-red-600'}`;
}

function updateGpsStatus(active) {
  elements.gpsIcon.textContent = active ? '✅' : '📍';
  elements.gpsStatus.textContent = active ? 'GPS Activo' : 'GPS Inactivo';
  elements.gpsStatus.className = `text-xs sm:text-sm font-medium ${active ? 'text-green-600' : 'text-gray-600'}`;
}

function updateUsersList() {
  elements.userCount.textContent = users.length;
  
  if (users.length === 0) {
    elements.usersList.innerHTML = '<p class="text-gray-500 text-center py-8">No hay usuarios conectados</p>';
    return;
  }

  elements.usersList.innerHTML = users.map(user => {
    const displayName = user.userName || user.userId.substring(0, 15);
    const isCurrentUser = user.userId === userId;
    
    let direction = '';
    if (user.bearing !== undefined) {
      const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SO', 'O', 'NO'];
      const index = Math.round(user.bearing / 45) % 8;
      direction = dirs[index];
    }
    
    return `
    <div class="p-4 rounded-lg border-2 ${isCurrentUser ? 'bg-indigo-50 border-indigo-300' : 'bg-gray-50 border-gray-200'}">
      <div class="flex items-center justify-between">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-lg font-semibold text-gray-800">${displayName}</span>
            ${isCurrentUser ? '<span class="text-xs bg-indigo-500 text-white px-2 py-1 rounded-full">Tú</span>' : ''}
          </div>
          <div class="text-xs text-gray-500 font-mono truncate">ID: ${user.userId.substring(0, 20)}...</div>
          <div class="flex items-center gap-3 mt-2 text-sm text-gray-600">
            <span>🧭 ${direction || '--'}</span>
            <span>🏆 ${user.maxSpeed || 0} km/h</span>
            <span class="text-xs">${new Date(user.timestamp).toLocaleTimeString()}</span>
          </div>
        </div>
        <div class="text-right ml-4">
          <div class="text-3xl font-bold text-indigo-600">${user.speed}</div>
          <div class="text-sm text-gray-600">km/h</div>
        </div>
      </div>
    </div>
    `;
  }).join('');
}

// ===== WEBSOCKET =====
function connectWebSocket() {
  console.log('🔌 Conectando a:', serverUrl);
  
  try {
    ws = new WebSocket(serverUrl);
    
    ws.onopen = () => {
      updateConnectionStatus(true);
      showSuccess('✓ Conectado al servidor');
      reconnectAttempts = 0;
      
      ws.send(JSON.stringify({ 
        type: 'register', 
        userId,
        userName: userName || 'Usuario',
        groupName: groupName || 'default'
      }));
      console.log('✓ Registrado en servidor - Grupo:', groupName || 'default');
      
      startKeepAlive();
    };

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'users') {
        users = data.users;
        updateUsersList();
        updateMap();
      } else if (data.type === 'pong') {
        console.log('💓 Pong recibido');
      }
    };

    ws.onerror = (error) => {
      console.error('❌ Error WebSocket:', error);
      showError('⚠️ Error de conexión. Verifica el servidor.');
      updateConnectionStatus(false);
    };

    ws.onclose = () => {
      console.log('🔌 Conexión cerrada');
      updateConnectionStatus(false);
      stopKeepAlive();
      
      if (isTracking && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
        console.log(`↻ Reconectando en ${delay/1000}s (intento ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`);
        setTimeout(connectWebSocket, delay);
      }
    };
  } catch (error) {
    console.error('❌ Error al crear WebSocket:', error);
    showError('No se pudo conectar: ' + error.message);
  }
}

function startKeepAlive() {
  stopKeepAlive();
  keepAliveInterval = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      console.log('💓 Keep-alive ping');
      ws.send(JSON.stringify({ type: 'ping' }));
    }
  }, 25000);
}

function stopKeepAlive() {
  if (keepAliveInterval) {
    clearInterval(keepAliveInterval);
    keepAliveInterval = null;
  }
}

// ===== GPS =====
function calculateBearing(lat1, lon1, lat2, lon2) {
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const y = Math.sin(dLon) * Math.cos(lat2 * Math.PI / 180);
  const x = Math.cos(lat1 * Math.PI / 180) * Math.sin(lat2 * Math.PI / 180) -
            Math.sin(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.cos(dLon);
  const bearing = Math.atan2(y, x) * 180 / Math.PI;
  return (bearing + 360) % 360;
}

function startGPSTracking() {
  const options = {
    enableHighAccuracy: true,
    timeout: 15000,
    maximumAge: 0
  };

  let lastPosition = null;

  watchId = navigator.geolocation.watchPosition(
    (position) => {
      const speed = position.coords.speed !== null 
        ? Math.max(0, Math.round(position.coords.speed * 3.6))
        : 0;
      
      elements.speedDisplay.textContent = speed;
      updateGpsStatus(true);
      elements.gpsInfo.classList.remove('hidden');
      
      if (speed > maxSpeed) {
        maxSpeed = speed;
        elements.maxSpeed.textContent = maxSpeed;
      }
      
      speedReadings.push(speed);
      if (speedReadings.length > 20) speedReadings.shift();
      const avgSpeed = Math.round(speedReadings.reduce((a, b) => a + b, 0) / speedReadings.length);
      elements.avgSpeed.textContent = avgSpeed;
      
      let bearing = 0;
      if (lastPosition && position.coords.speed > 1) {
        bearing = calculateBearing(
          lastPosition.coords.latitude,
          lastPosition.coords.longitude,
          position.coords.latitude,
          position.coords.longitude
        );
      }
      
      const accuracy = Math.round(position.coords.accuracy);
      elements.accuracy.textContent = accuracy;
      elements.coords.textContent = `${position.coords.latitude.toFixed(6)}, ${position.coords.longitude.toFixed(6)}`;

      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({
          type: 'speed',
          userId,
          userName: userName || 'Usuario',
          groupName: groupName || 'default',
          speed,
          maxSpeed,
          lat: position.coords.latitude,
          lon: position.coords.longitude,
          bearing: Math.round(bearing),
          timestamp: Date.now()
        }));
      }
      
      lastPosition = position;
    },
    (error) => {
      let errorMsg = '';
      switch(error.code) {
        case error.PERMISSION_DENIED:
          errorMsg = '❌ Permisos denegados. Ve a Configuración → Permisos → Ubicación';
          break;
        case error.POSITION_UNAVAILABLE:
          errorMsg = '⚠️ GPS no disponible. Sal al exterior.';
          break;
        case error.TIMEOUT:
          errorMsg = '⏱️ Tiempo agotado. Verifica que el GPS esté activado.';
          break;
        default:
          errorMsg = '❌ Error GPS: ' + error.message;
      }
      showError(errorMsg);
      updateGpsStatus(false);
    },
    options
  );
}

// ===== WAKE LOCK =====
async function requestWakeLock() {
  if ('wakeLock' in navigator) {
    try {
      wakeLock = await navigator.wakeLock.request('screen');
      console.log('✓ Wake Lock activado');
    } catch (err) {
      console.log('Wake Lock error:', err);
    }
  }
}

function releaseWakeLock() {
  if (wakeLock) {
    wakeLock.release();
    wakeLock = null;
    console.log('Wake Lock liberado');
  }
}

// ===== CONTROL DE SEGUIMIENTO =====
async function startTracking() {
  console.log('🎯 INICIANDO SEGUIMIENTO');

  if (!userName || userName.trim() === '') {
    showError('⚠️ Por favor, introduce tu nombre antes de comenzar');
    elements.userName.focus();
    return;
  }

  if (!groupName || groupName.trim() === '') {
    showError('⚠️ Por favor, introduce el nombre del grupo');
    elements.groupName.focus();
    return;
  }
  
  if (!navigator.geolocation) {
    showError('❌ GPS no disponible en este dispositivo');
    return;
  }

  await requestWakeLock();

  // CAMBIAR VISIBILIDAD
  elements.configSection.classList.add('hidden');
  elements.configSection.style.display = 'none';
  elements.statusSection.classList.remove('hidden');
  elements.statusSection.style.display = 'block';
  
  connectWebSocket();
  startGPSTracking();

  isTracking = true;
  console.log('✅ SEGUIMIENTO INICIADO');
}

function stopTracking() {
  console.log('⏹️ DETENIENDO SEGUIMIENTO');
  
  if (watchId) {
    navigator.geolocation.clearWatch(watchId);
    watchId = null;
  }
  
  if (ws) {
    ws.close();
    ws = null;
  }
  
  stopKeepAlive();
  releaseWakeLock();
  
  isTracking = false;
  reconnectAttempts = 0;
  updateGpsStatus(false);
  updateConnectionStatus(false);
  elements.speedDisplay.textContent = '0';
  elements.gpsInfo.style.display = 'none';
  
  elements.configSection.classList.remove('hidden');
  elements.configSection.style.display = 'block';
  elements.statusSection.classList.add('hidden');
  elements.statusSection.style.display = 'none';
  
  elements.trackButton.textContent = '▶️ Iniciar Seguimiento';
  elements.trackButton.className = 'flex-1 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-semibold py-3 px-6 rounded-lg transition-colors';
  
  console.log('✅ SEGUIMIENTO DETENIDO');
}

function resetStats() {
  maxSpeed = 0;
  speedReadings = [];
  elements.maxSpeed.textContent = '0';
  elements.avgSpeed.textContent = '0';
  showSuccess('✓ Estadísticas reseteadas');
}

// ===== MAPA =====
function initMap(lat, lon, containerId) {
  const container = document.getElementById(containerId);
  const isFs = containerId.includes('fullscreen');
  
  if (isFs) {
    if (mapFullscreen) return;
    mapFullscreen = L.map(containerId).setView([lat, lon], 15);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(mapFullscreen);
  } else {
    if (map) return;
    map = L.map(containerId).setView([lat, lon], 15);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(map);
  }
}

function createArrowIcon(speed, isCurrentUser, bearing = 0) {
  const color = isCurrentUser ? '#4F46E5' : '#10B981';
  const size = isCurrentUser ? 40 : 35;
  
  const html = `
    <div style="position: relative; width: ${size}px; height: ${size}px;">
      <div style="position: absolute; top: -25px; left: 50%; transform: translateX(-50%); background: ${color}; color: white; padding: 3px 8px; border-radius: 5px; font-size: 12px; font-weight: bold; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.3); z-index: 1000;">
        ${speed} km/h
      </div>
      <svg width="${size}" height="${size}" viewBox="0 0 24 24" style="transform: rotate(${bearing}deg); filter: drop-shadow(0 2px 4px rgba(0,0,0,0.4));">
        <path d="M12 2 L4 20 L12 16 L20 20 Z" fill="${color}" stroke="white" stroke-width="1.5"/>
      </svg>
    </div>
  `;
  
  return L.divIcon({
    html: html,
    className: '',
    iconSize: [size, size],
    iconAnchor: [size/2, size/2],
    popupAnchor: [0, -size/2]
  });
}

function updateMap() {
  const activeMap = isFullscreen ? mapFullscreen : map;
  if (!activeMap || (!mapVisible && !isFullscreen)) return;

  users.forEach(user => {
    const isCurrentUser = user.userId === userId;
    const markerId = user.userId + (isFullscreen ? '_fs' : '_normal');
    
    if (!userMarkers[markerId]) {
      try {
        const icon = createArrowIcon(user.speed, isCurrentUser, user.bearing || 0);
        const marker = L.marker([user.lat, user.lon], { icon: icon }).addTo(activeMap);
        
        const displayName = user.userName || user.userId.substring(0, 15);
        marker.bindPopup(`
          <div style="text-align: center;">
            <strong>${displayName}</strong><br>
            <span style="font-size: 18px; font-weight: bold;">${user.speed} km/h</span><br>
            <span style="font-size: 14px;">🏆 Max: ${user.maxSpeed || 0} km/h</span><br>
            <span style="font-size: 11px; color: #666;">${new Date(user.timestamp).toLocaleTimeString()}</span>
          </div>
        `);
        
        userMarkers[markerId] = marker;
        userTracks[markerId] = [];
      } catch (error) {
        console.error('Error creando marcador:', error);
      }
    } else {
      try {
        const marker = userMarkers[markerId];
        marker.setLatLng([user.lat, user.lon]);
        marker.setIcon(createArrowIcon(user.speed, isCurrentUser, user.bearing || 0));
      } catch (error) {
        console.error('Error actualizando marcador:', error);
      }
    }
    
    if (user.speed > 3.6) {
      const track = userTracks[markerId];
      if (track) {
        track.push([user.lat, user.lon]);
        if (track.length > 50) track.shift();
        
        const polylineId = markerId + '_polyline';
        if (trackPolylines[polylineId]) {
          activeMap.removeLayer(trackPolylines[polylineId]);
        }
        
        if (track.length > 1) {
          const color = isCurrentUser ? '#4F46E5' : '#10B981';
          trackPolylines[polylineId] = L.polyline(track, {
            color: color,
            weight: 3,
            opacity: 0.7
          }).addTo(activeMap);
        }
      }
    }
  });

  const currentUser = users.find(u => u.userId === userId);
  if (currentUser) {
    activeMap.setView([currentUser.lat, currentUser.lon], activeMap.getZoom());
  }
}

function clearMapTracks() {
  const activeMap = isFullscreen ? mapFullscreen : map;
  if (!activeMap) return;
  
  Object.keys(trackPolylines).forEach(polylineId => {
    if (trackPolylines[polylineId]) {
      activeMap.removeLayer(trackPolylines[polylineId]);
      delete trackPolylines[polylineId];
    }
  });
  
  Object.keys(userTracks).forEach(trackId => {
    userTracks[trackId] = [];
  });
  
  showSuccess('✓ Trayectorias limpiadas');
}

// ===== EVENT LISTENERS =====
elements.trackButton.addEventListener('click', startTracking);
elements.stopButton.addEventListener('click', stopTracking);
elements.resetStatsButton.addEventListener('click', resetStats);

elements.toggleMapButton.addEventListener('click', () => {
  mapVisible = true;
  elements.mapContainer.classList.remove('hidden');
  
  const currentUser = users.find(u => u.userId === userId);
  if (currentUser) {
    initMap(currentUser.lat, currentUser.lon, 'map');
  } else if (users.length > 0) {
    initMap(users[0].lat, users[0].lon, 'map');
  } else {
    initMap(40.4168, -3.7038, 'map');
  }
  
  setTimeout(() => {
    if (map) map.invalidateSize();
    updateMap();
  }, 100);
});

elements.closeMapButton.addEventListener('click', () => {
  mapVisible = false;
  elements.mapContainer.classList.add('hidden');
});

elements.clearTracksButton.addEventListener('click', clearMapTracks);

elements.fullscreenButton.addEventListener('click', () => {
  isFullscreen = true;
  elements.mapContainer.classList.add('hidden');
  elements.mapFullscreen.classList.remove('hidden');
  
  const firstUser = users.length > 0 ? users[0] : null;
  if (firstUser) {
    initMap(firstUser.lat, firstUser.lon, 'map-fullscreen-container');
  } else {
    initMap(40.4168, -3.7038, 'map-fullscreen-container');
  }
  
  setTimeout(() => {
    if (mapFullscreen) {
      mapFullscreen.invalidateSize();
      updateMap();
    }
  }, 200);
});

elements.exitFullscreenButton.addEventListener('click', () => {
  isFullscreen = false;
  elements.mapFullscreen.classList.add('hidden');
  elements.mapContainer.classList.remove('hidden');
  
  setTimeout(() => {
    if (map) {
      map.invalidateSize();
      updateMap();
    }
  }, 100);
});

elements.settingsButton.addEventListener('click', () => {
  elements.settingsModal.classList.remove('hidden');
});

elements.closeSettings.addEventListener('click', () => {
  elements.settingsModal.classList.add('hidden');
});

console.log('✓ GPS Tracker listo');