// server.js - Servidor WebSocket para GPS Speed Tracker
const WebSocket = require('ws');
const express = require('express');
const cors = require('cors');
const db = require('./database');

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(express.json());

// Inicializar base de datos
db.initializeDatabase().catch(err => {
  console.error('❌ Error fatal al inicializar base de datos:', err);
  console.warn('⚠️  El servidor continuará sin persistencia de datos');
});

// Crear servidor HTTP
const server = app.listen(PORT, () => {
  console.log(`🚀 Servidor HTTP corriendo en http://localhost:${PORT}`);
});

// Crear servidor WebSocket
const wss = new WebSocket.Server({ server });

// Almacenar usuarios conectados por grupo
const groups = new Map(); // Map<groupName, Map<userId, userData>>

// Rate limiting para bocina grupal (userId -> último timestamp)
const hornRateLimit = new Map(); // Map<userId, timestamp>

// ============================================
// CONSTANTS
// ============================================
const HORN_COOLDOWN = 5000;          // 5 s cooldown between horns
const INACTIVE_USER_TIMEOUT = 10000; // 10 s without update → remove user
const CLEANUP_INTERVAL = 5000;       // Run cleanup every 5 s
const HORN_RATELIMIT_CLEANUP_INTERVAL = 60000; // Clean up horn rate limit map every 60 s
const KML_STALE_THRESHOLD = 30000;   // 30 s → user considered stale in KML
const MAX_QUERY_LIMIT = 500;         // Maximum records per paginated query
const MAX_NAME_LENGTH = 64;          // Maximum length for names / group names
const MAX_USER_ID_LENGTH = 128;      // Maximum length for user IDs
const VALID_COORD_LAT = [-90, 90];
const VALID_COORD_LON = [-180, 180];

// ============================================
// HELPERS
// ============================================

// Escape characters that are special in XML/HTML contexts
function escapeXml(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

// Validate that a value is a finite number within [min, max]
function isValidNumber(val, min, max) {
  const n = Number(val);
  return Number.isFinite(n) && n >= min && n <= max;
}

// Clamp and parse the `limit` query param to a safe range
function parseLimit(raw, defaultVal = 100) {
  const n = parseInt(raw, 10);
  if (!Number.isFinite(n) || n <= 0) return defaultVal;
  return Math.min(n, MAX_QUERY_LIMIT);
}

// Función para broadcast a un grupo específico
function broadcastToGroup(groupName, data) {
  const message = JSON.stringify(data);
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN && client.groupName === groupName) {
      client.send(message);
    }
  });
}

// Función para enviar lista actualizada de usuarios a un grupo
function sendUsersListToGroup(groupName) {
  const groupUsers = groups.get(groupName);
  if (!groupUsers) return;
  
  const usersList = Array.from(groupUsers.values());
  broadcastToGroup(groupName, {
    type: 'users',
    users: usersList
  });
}

// Limpiar usuarios inactivos (más de 10 segundos sin actualizar)
setInterval(() => {
  const now = Date.now();

  groups.forEach((groupUsers, groupName) => {
    // Track changes per-group so only affected groups get a broadcast
    let groupHasChanges = false;

    groupUsers.forEach((user, userId) => {
      if (now - user.timestamp > INACTIVE_USER_TIMEOUT) {
        groupUsers.delete(userId);
        groupHasChanges = true;
        console.log(`❌ Usuario inactivo eliminado: ${userId} (Grupo: ${groupName})`);
      }
    });

    // Eliminar grupo si está vacío
    if (groupUsers.size === 0) {
      groups.delete(groupName);
      console.log(`🗑️ Grupo vacío eliminado: ${groupName}`);
    } else if (groupHasChanges) {
      sendUsersListToGroup(groupName);
    }
  });
}, CLEANUP_INTERVAL);

// Limpiar entradas antiguas del mapa de rate-limiting de bocina
setInterval(() => {
  const now = Date.now();
  hornRateLimit.forEach((timestamp, userId) => {
    if (now - timestamp > HORN_COOLDOWN * 2) {
      hornRateLimit.delete(userId);
    }
  });
}, HORN_RATELIMIT_CLEANUP_INTERVAL);

// Manejar conexiones WebSocket
wss.on('connection', (ws, req) => {
  console.log('✅ Nueva conexión WebSocket desde:', req.socket.remoteAddress);
  
  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);
      
      switch (data.type) {
        case 'register': {
          const groupName = (data.groupName || 'default').toString().slice(0, MAX_NAME_LENGTH);
          const userId = (data.userId || '').toString().slice(0, MAX_USER_ID_LENGTH);
          const userName = (data.userName || 'Usuario').toString().slice(0, MAX_NAME_LENGTH);

          if (!userId) {
            ws.send(JSON.stringify({ type: 'error', message: 'userId requerido' }));
            break;
          }

          ws.userId = userId;
          ws.userName = userName;
          ws.groupName = groupName;

          console.log(`📝 Usuario registrado: ${userName} (${userId}) (Grupo: ${groupName})`);

          // Crear grupo si no existe
          if (!groups.has(groupName)) {
            groups.set(groupName, new Map());
            console.log(`✨ Nuevo grupo creado: ${groupName}`);
          }

          sendUsersListToGroup(groupName);
          break;
        }

        case 'join': {
          // Modo visualizador - solo escuchar, no registrar como usuario
          const viewerGroup = (data.groupName || 'default').toString().slice(0, MAX_NAME_LENGTH);
          ws.groupName = viewerGroup;
          ws.viewerMode = true;

          console.log(`👁️ Visualizador conectado al grupo: ${viewerGroup}`);

          // Enviar lista actual de usuarios
          sendUsersListToGroup(viewerGroup);
          break;
        }

        case 'speed': {
          const group = (data.groupName || 'default').toString().slice(0, MAX_NAME_LENGTH);
          const speedUserId = (data.userId || '').toString().slice(0, MAX_USER_ID_LENGTH);
          const speedUserName = (data.userName || 'Usuario').toString().slice(0, MAX_NAME_LENGTH);

          // Validate coordinates and speed values
          if (!speedUserId ||
              !isValidNumber(data.lat, VALID_COORD_LAT[0], VALID_COORD_LAT[1]) ||
              !isValidNumber(data.lon, VALID_COORD_LON[0], VALID_COORD_LON[1]) ||
              !isValidNumber(data.speed, 0, 999)) {
            ws.send(JSON.stringify({ type: 'error', message: 'Datos de velocidad inválidos' }));
            break;
          }

          // Asegurar que el grupo existe
          if (!groups.has(group)) {
            groups.set(group, new Map());
          }

          const groupUsers = groups.get(group);
          const currentUser = groupUsers.get(speedUserId);
          const incomingMaxSpeed = isValidNumber(data.maxSpeed, 0, 999) ? data.maxSpeed : data.speed;
          const newMaxSpeed = currentUser
            ? Math.max(currentUser.maxSpeed || 0, incomingMaxSpeed)
            : incomingMaxSpeed;
          const bearing = isValidNumber(data.bearing, 0, 360) ? data.bearing : 0;
          const timestamp = Number.isFinite(Number(data.timestamp)) ? data.timestamp : Date.now();

          // Actualizar datos del usuario en su grupo
          groupUsers.set(speedUserId, {
            userId: speedUserId,
            userName: speedUserName,
            speed: Number(data.speed),
            maxSpeed: newMaxSpeed,
            lat: Number(data.lat),
            lon: Number(data.lon),
            bearing,
            timestamp
          });

          console.log(`📊 [${group}] ${speedUserName}: ${data.speed} km/h | Rumbo: ${bearing}° | Max: ${newMaxSpeed} km/h`);

          // Enviar lista actualizada solo a usuarios del mismo grupo
          sendUsersListToGroup(group);
          break;
        }

        case 'ping':
          // Responder al keep-alive ping
          ws.send(JSON.stringify({ type: 'pong' }));
          break;

        case 'group-horn': {
          const hornGroup = (data.groupName || 'default').toString().slice(0, MAX_NAME_LENGTH);
          const hornUserId = (data.userId || '').toString().slice(0, MAX_USER_ID_LENGTH);
          const hornUserName = (data.userName || 'Usuario').toString().slice(0, MAX_NAME_LENGTH);
          const now = Date.now();

          if (!hornUserId) {
            ws.send(JSON.stringify({ type: 'error', message: 'userId requerido para bocina' }));
            break;
          }

          // Validar que el grupo existe
          if (!groups.has(hornGroup)) {
            console.log(`⚠️ Intento de bocina en grupo inexistente: ${hornGroup}`);
            break;
          }

          // Rate limiting: verificar cooldown
          const lastHornTime = hornRateLimit.get(hornUserId);
          if (lastHornTime && (now - lastHornTime) < HORN_COOLDOWN) {
            const remainingTime = Math.ceil((HORN_COOLDOWN - (now - lastHornTime)) / 1000);
            console.log(`⏱️ Rate limit: ${hornUserName} debe esperar ${remainingTime}s para usar la bocina`);
            ws.send(JSON.stringify({
              type: 'error',
              message: `Debes esperar ${remainingTime} segundos antes de usar la bocina nuevamente`
            }));
            break;
          }

          // Actualizar timestamp del último uso
          hornRateLimit.set(hornUserId, now);

          // Log de auditoría
          console.log(`📢 Bocina activada por ${hornUserName} (${hornUserId}) en grupo ${hornGroup}`);

          // Distribuir mensaje a todos los usuarios del mismo grupo
          broadcastToGroup(hornGroup, {
            type: 'group-horn',
            userId: hornUserId,
            userName: hornUserName,
            groupName: hornGroup,
            timestamp: data.timestamp || now
          });
          break;
        }

        default:
          console.log('⚠️ Tipo de mensaje desconocido:', data.type);
      }
    } catch (error) {
      console.error('❌ Error procesando mensaje:', error);
    }
  });
  
  ws.on('close', () => {
    if (ws.groupName && !ws.viewerMode && ws.userId) {
      console.log(`👋 Usuario desconectado: ${ws.userId} (Grupo: ${ws.groupName})`);

      const groupUsers = groups.get(ws.groupName);
      if (groupUsers) {
        groupUsers.delete(ws.userId);

        // Si el grupo queda vacío, eliminarlo
        if (groupUsers.size === 0) {
          groups.delete(ws.groupName);
          console.log(`🗑️ Grupo vacío eliminado: ${ws.groupName}`);
        } else {
          sendUsersListToGroup(ws.groupName);
        }
      }

      // Limpiar rate limiting del usuario desconectado
      hornRateLimit.delete(ws.userId);
    } else if (ws.viewerMode) {
      console.log(`👋 Visualizador desconectado del grupo: ${ws.groupName}`);
    }
  });
  
  ws.on('error', (error) => {
    console.error('❌ Error en WebSocket:', error);
  });
});

// Endpoint de salud
app.get('/health', (req, res) => {
  let totalUsers = 0;
  groups.forEach(groupUsers => {
    totalUsers += groupUsers.size;
  });
  
  res.json({
    status: 'ok',
    totalUsers: totalUsers,
    totalGroups: groups.size,
    timestamp: Date.now()
  });
});

// Endpoint para obtener grupos y usuarios
app.get('/groups', (req, res) => {
  const groupsInfo = {};
  groups.forEach((groupUsers, groupName) => {
    groupsInfo[groupName] = {
      userCount: groupUsers.size,
      users: Array.from(groupUsers.values())
    };
  });
  
  res.json({
    groups: groupsInfo,
    totalGroups: groups.size
  });
});

// Endpoint para obtener usuarios de un grupo específico
app.get('/groups/:groupName', (req, res) => {
  const groupName = req.params.groupName;
  const groupUsers = groups.get(groupName);

  if (!groupUsers) {
    return res.status(404).json({ error: 'Grupo no encontrado' });
  }

  res.json({
    groupName: groupName,
    users: Array.from(groupUsers.values()),
    count: groupUsers.size
  });
});

// ============================================
// SPEED HISTORY API ENDPOINTS
// ============================================

// POST endpoint to save a speed history record
app.post('/api/speed-history', async (req, res) => {
  try {
    const { userId, userName, groupName, maxSpeed, latitude, longitude, timestamp } = req.body;

    // Validate required fields
    if (!userId || maxSpeed === undefined || !latitude || !longitude || !timestamp) {
      return res.status(400).json({
        error: 'Faltan campos requeridos',
        required: ['userId', 'maxSpeed', 'latitude', 'longitude', 'timestamp']
      });
    }

    // Extract date and time from timestamp
    const dateObj = new Date(timestamp);
    const date = dateObj.toISOString().split('T')[0]; // YYYY-MM-DD
    const time = dateObj.toTimeString().split(' ')[0]; // HH:MM:SS

    const record = await db.insertSpeedHistory({
      userId,
      userName: userName || 'Usuario',
      groupName: groupName || 'default',
      maxSpeed,
      latitude,
      longitude,
      date,
      time,
      timestamp
    });

    console.log(`💾 Registro de velocidad guardado: ${userName || userId} - ${maxSpeed} km/h`);

    res.status(201).json({
      success: true,
      record
    });
  } catch (error) {
    console.error('❌ Error guardando registro de velocidad:', error);
    res.status(500).json({
      error: 'Error interno del servidor',
      message: error.message
    });
  }
});

// GET endpoint to retrieve speed history for a specific user
app.get('/api/speed-history/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const limit = parseLimit(req.query.limit, 100);
    const offset = Math.max(0, parseInt(req.query.offset, 10) || 0);

    const history = await db.getSpeedHistory(userId, limit, offset);

    res.json({
      success: true,
      userId,
      count: history.length,
      records: history
    });
  } catch (error) {
    console.error('❌ Error obteniendo historial:', error);
    res.status(500).json({
      error: 'Error interno del servidor',
      message: error.message
    });
  }
});

// GET endpoint to retrieve speed statistics for a user
app.get('/api/speed-history/:userId/stats', async (req, res) => {
  try {
    const { userId } = req.params;
    const stats = await db.getSpeedStatistics(userId);

    res.json({
      success: true,
      userId,
      statistics: stats
    });
  } catch (error) {
    console.error('❌ Error obteniendo estadísticas:', error);
    res.status(500).json({
      error: 'Error interno del servidor',
      message: error.message
    });
  }
});

// GET endpoint to retrieve all speed history (admin)
app.get('/api/speed-history', async (req, res) => {
  try {
    const limit = parseLimit(req.query.limit, 100);
    const offset = Math.max(0, parseInt(req.query.offset, 10) || 0);

    const history = await db.getAllSpeedHistory(limit, offset);

    res.json({
      success: true,
      count: history.length,
      records: history
    });
  } catch (error) {
    console.error('❌ Error obteniendo todo el historial:', error);
    res.status(500).json({
      error: 'Error interno del servidor',
      message: error.message
    });
  }
});

// ============================================
// WAYPOINTS API ENDPOINTS
// ============================================

// POST endpoint to create a new waypoint
app.post('/api/waypoints', async (req, res) => {
  try {
    const { groupName, name, description, latitude, longitude, createdBy } = req.body;

    // Validate required fields
    if (!groupName || !name || latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        error: 'Missing required fields',
        required: ['groupName', 'name', 'latitude', 'longitude']
      });
    }

    const waypoint = await db.createWaypoint({
      groupName,
      name,
      description,
      latitude,
      longitude,
      createdBy
    });

    console.log(`📍 Waypoint created: ${name} for group ${groupName}`);

    res.status(201).json({
      success: true,
      waypoint
    });
  } catch (error) {
    console.error('❌ Error creating waypoint:', error);
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

// GET endpoint to retrieve all waypoints for a group
app.get('/api/waypoints/:groupName', async (req, res) => {
  try {
    const { groupName } = req.params;
    const waypoints = await db.getWaypointsByGroup(groupName);

    res.json({
      success: true,
      groupName,
      count: waypoints.length,
      waypoints
    });
  } catch (error) {
    console.error('❌ Error getting waypoints:', error);
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

// PUT endpoint to update a waypoint
app.put('/api/waypoints/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, description, latitude, longitude } = req.body;

    // Validate required fields
    if (!name || latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        error: 'Missing required fields',
        required: ['name', 'latitude', 'longitude']
      });
    }

    const waypoint = await db.updateWaypoint(id, {
      name,
      description,
      latitude,
      longitude
    });

    console.log(`📝 Waypoint updated: ${name} (ID: ${id})`);

    res.json({
      success: true,
      waypoint
    });
  } catch (error) {
    console.error('❌ Error updating waypoint:', error);
    if (error.message === 'Waypoint not found') {
      return res.status(404).json({
        error: 'Waypoint not found'
      });
    }
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

// DELETE endpoint to delete a waypoint
app.delete('/api/waypoints/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const waypoint = await db.deleteWaypoint(id);

    console.log(`🗑️ Waypoint deleted: ${waypoint.name} (ID: ${id})`);

    res.json({
      success: true,
      waypoint
    });
  } catch (error) {
    console.error('❌ Error deleting waypoint:', error);
    if (error.message === 'Waypoint not found') {
      return res.status(404).json({
        error: 'Waypoint not found'
      });
    }
    res.status(500).json({
      error: 'Internal server error',
      message: error.message
    });
  }
});

// ============================================
// KML NETWORK LINK ENDPOINTS
// ============================================

// Function to generate KML XML for active users
function generateUsersKML(groupName = null) {
  const now = Date.now();
  let allUsers = [];

  // Collect users from all groups or specific group
  if (groupName) {
    const groupUsers = groups.get(groupName);
    if (groupUsers) {
      allUsers = Array.from(groupUsers.values());
    }
  } else {
    // Collect all users from all groups
    groups.forEach((groupUsers, gName) => {
      groupUsers.forEach(user => {
        allUsers.push({ ...user, groupName: gName });
      });
    });
  }

  // Filter out stale users (older than KML_STALE_THRESHOLD)
  const activeUsers = allUsers.filter(user => (now - user.timestamp) < KML_STALE_THRESHOLD);

  // Generate KML placemarks for each user
  const placemarks = activeUsers.map(user => {
    const age = Math.floor((now - user.timestamp) / 1000);
    const ageStatus = age < 5 ? '🟢' : age < 10 ? '🟡' : '🟠';

    // Escape user-provided strings used in XML element content
    const safeName = escapeXml(user.userName);
    const safeUserId = escapeXml(user.userId);
    const safeGroupName = escapeXml(user.groupName || 'default');

    return `
    <Placemark>
      <name>${ageStatus} ${safeName}</name>
      <description><![CDATA[
        <b>User:</b> ${user.userName} (${user.userId})<br/>
        <b>Group:</b> ${user.groupName || 'default'}<br/>
        <b>Speed:</b> ${user.speed.toFixed(1)} km/h<br/>
        <b>Max Speed:</b> ${user.maxSpeed.toFixed(1)} km/h<br/>
        <b>Bearing:</b> ${user.bearing.toFixed(0)}°<br/>
        <b>Last Update:</b> ${age}s ago<br/>
        <b>Time:</b> ${new Date(user.timestamp).toISOString()}
      ]]></description>
      <ExtendedData>
        <Data name="userId"><value>${safeUserId}</value></Data>
        <Data name="groupName"><value>${safeGroupName}</value></Data>
      </ExtendedData>
      <styleUrl>#userStyle</styleUrl>
      <Point>
        <coordinates>${user.lon},${user.lat},0</coordinates>
      </Point>
    </Placemark>`;
  }).join('\n');

  const kml = `<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>GPS Tracker - Active Users${groupName ? ` (${escapeXml(groupName)})` : ''}</name>
    <description>Real-time positions of active GPS tracker users</description>

    <!-- Define styles for user markers -->
    <Style id="userStyle">
      <IconStyle>
        <color>ff00ff00</color>
        <scale>1.2</scale>
        <Icon>
          <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>
        </Icon>
      </IconStyle>
      <LabelStyle>
        <scale>0.9</scale>
      </LabelStyle>
    </Style>

    ${placemarks || '<Placemark><name>No active users</name><description>No users currently active</description></Placemark>'}
  </Document>
</kml>`;

  return kml;
}

// GET endpoint for KML Network Link (root document with auto-refresh)
app.get('/kml/network-link', (req, res) => {
  const groupName = req.query.group ? req.query.group.toString().slice(0, MAX_NAME_LENGTH) : null;
  const refreshInterval = Math.max(2, Math.min(60, parseInt(req.query.refresh, 10) || 5)); // 2–60 s

  // Get the host from the request to construct the full URL
  const protocol = req.secure ? 'https' : 'http';
  const host = req.get('host');
  const dataUrl = `${protocol}://${host}/kml/users${groupName ? `?group=${encodeURIComponent(groupName)}` : ''}`;

  const kml = `<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>GPS Tracker - Live Network Link${groupName ? ` (${groupName})` : ''}</name>
    <description>Auto-refreshing network link showing real-time GPS positions</description>

    <NetworkLink>
      <name>Active Users</name>
      <description>Updates every ${refreshInterval} seconds</description>
      <refreshVisibility>1</refreshVisibility>
      <flyToView>0</flyToView>
      <Link>
        <href>${dataUrl}</href>
        <refreshMode>onInterval</refreshMode>
        <refreshInterval>${refreshInterval}</refreshInterval>
      </Link>
    </NetworkLink>
  </Document>
</kml>`;

  res.set('Content-Type', 'application/vnd.google-earth.kml+xml');
  res.send(kml);

  console.log(`🗺️  KML Network Link solicitado${groupName ? ` (Grupo: ${groupName})` : ' (Todos los grupos)'}`);
});

// GET endpoint for KML user data (refreshed by NetworkLink)
app.get('/kml/users', (req, res) => {
  const groupName = req.query.group;

  const kml = generateUsersKML(groupName);

  res.set('Content-Type', 'application/vnd.google-earth.kml+xml');
  res.send(kml);

  console.log(`🗺️  KML data generado${groupName ? ` (Grupo: ${groupName})` : ' (Todos los grupos)'}`);
});

console.log(`🌐 Servidor WebSocket corriendo en ws://localhost:${PORT}`);
console.log(`📡 Los clientes deben conectarse a: ws://localhost:${PORT}`);
console.log(`\n💡 Endpoints disponibles:`);
console.log(`   - GET /health - Estado del servidor`);
console.log(`   - GET /groups - Lista de todos los grupos`);
console.log(`   - GET /groups/:groupName - Usuarios de un grupo específico`);
console.log(`\n📊 Speed History API:`);
console.log(`   - POST /api/speed-history - Guardar registro de velocidad máxima`);
console.log(`   - GET /api/speed-history/:userId - Obtener historial de un usuario`);
console.log(`   - GET /api/speed-history/:userId/stats - Obtener estadísticas de un usuario`);
console.log(`   - GET /api/speed-history - Obtener todo el historial (admin)`);
console.log(`\n📍 Waypoints API:`);
console.log(`   - POST /api/waypoints - Crear un waypoint`);
console.log(`   - GET /api/waypoints/:groupName - Obtener todos los waypoints de un grupo`);
console.log(`   - PUT /api/waypoints/:id - Actualizar un waypoint`);
console.log(`   - DELETE /api/waypoints/:id - Eliminar un waypoint`);
console.log(`\n🗺️  KML Network Link API:`);
console.log(`   - GET /kml/network-link - KML con NetworkLink para auto-actualización`);
console.log(`     Query params: ?group=groupName (opcional), ?refresh=seconds (default 5)`);
console.log(`   - GET /kml/users - KML con datos actuales de usuarios`);
console.log(`     Query params: ?group=groupName (opcional)`);
console.log(`\n⚙️  Para usar desde otro dispositivo, reemplaza 'localhost' con la IP de este equipo`);
console.log(`\n🔐 Sistema de grupos activado - Los usuarios solo verán a otros de su mismo grupo\n`);
