// server.js - Servidor WebSocket para GPS Speed Tracker
const WebSocket = require('ws');
const express = require('express');
const cors = require('cors');
const db = require('./database');

const app = express();
const PORT = 3001;

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
const HORN_COOLDOWN = 5000; // 5 segundos de cooldown entre bocinas

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
  let hasChanges = false;
  
  groups.forEach((groupUsers, groupName) => {
    groupUsers.forEach((user, userId) => {
      if (now - user.timestamp > 10000) {
        groupUsers.delete(userId);
        hasChanges = true;
        console.log(`❌ Usuario inactivo eliminado: ${userId} (Grupo: ${groupName})`);
      }
    });
    
    // Eliminar grupo si está vacío
    if (groupUsers.size === 0) {
      groups.delete(groupName);
      console.log(`🗑️ Grupo vacío eliminado: ${groupName}`);
    } else if (hasChanges) {
      sendUsersListToGroup(groupName);
    }
  });
}, 5000);

// Manejar conexiones WebSocket
wss.on('connection', (ws, req) => {
  console.log('✅ Nueva conexión WebSocket desde:', req.socket.remoteAddress);
  
  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);
      
      switch (data.type) {
        case 'register':
          const groupName = data.groupName || 'default';
          ws.userId = data.userId;
          ws.userName = data.userName || 'Usuario';
          ws.groupName = groupName;
          
          console.log(`📝 Usuario registrado: ${data.userName || data.userId} (Grupo: ${groupName})`);
          
          // Crear grupo si no existe
          if (!groups.has(groupName)) {
            groups.set(groupName, new Map());
            console.log(`✨ Nuevo grupo creado: ${groupName}`);
          }
          
          sendUsersListToGroup(groupName);
          break;

        case 'join':
          // Modo visualizador - solo escuchar, no registrar como usuario
          const viewerGroup = data.groupName || 'default';
          ws.groupName = viewerGroup;
          ws.viewerMode = true;
          
          console.log(`👁️ Visualizador conectado al grupo: ${viewerGroup}`);
          
          // Enviar lista actual de usuarios
          sendUsersListToGroup(viewerGroup);
          break;
          
        case 'speed':
          const group = data.groupName || 'default';
          
          // Asegurar que el grupo existe
          if (!groups.has(group)) {
            groups.set(group, new Map());
          }
          
          const groupUsers = groups.get(group);
          const currentUser = groupUsers.get(data.userId);
          const newMaxSpeed = currentUser 
            ? Math.max(currentUser.maxSpeed || 0, data.maxSpeed || data.speed)
            : data.maxSpeed || data.speed;
          
          // Actualizar datos del usuario en su grupo
          groupUsers.set(data.userId, {
            userId: data.userId,
            userName: data.userName || 'Usuario',
            speed: data.speed,
            maxSpeed: newMaxSpeed,
            lat: data.lat,
            lon: data.lon,
            bearing: data.bearing || 0,
            timestamp: data.timestamp
          });
          
          console.log(`📊 [${group}] ${data.userName || data.userId}: ${data.speed} km/h | Rumbo: ${data.bearing}° | Max: ${newMaxSpeed} km/h`);
          
          // Enviar lista actualizada solo a usuarios del mismo grupo
          sendUsersListToGroup(group);
          break;

        case 'ping':
          // Responder al keep-alive ping
          ws.send(JSON.stringify({ type: 'pong' }));
          break;

        case 'group-horn':
          const hornGroup = data.groupName || 'default';
          const hornUserId = data.userId;
          const hornUserName = data.userName || 'Usuario';
          const now = Date.now();

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
    const limit = parseInt(req.query.limit) || 100;
    const offset = parseInt(req.query.offset) || 0;

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
    const limit = parseInt(req.query.limit) || 100;
    const offset = parseInt(req.query.offset) || 0;

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
console.log(`\n⚙️  Para usar desde otro dispositivo, reemplaza 'localhost' con la IP de este equipo`);
console.log(`\n🔐 Sistema de grupos activado - Los usuarios solo verán a otros de su mismo grupo\n`);
