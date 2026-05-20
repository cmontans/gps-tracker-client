const test = require('node:test');
const assert = require('node:assert');
const http = require('http');

// Capture the HTTP server started by server.js
let serverInstance = null;
const originalListen = http.Server.prototype.listen;
http.Server.prototype.listen = function(...args) {
  serverInstance = this;
  return originalListen.apply(this, args);
};

// Set PORT to 0 to run on a random free port and avoid port collision
process.env.PORT = '0';
process.env.DATABASE_URL = 'postgres://localhost/test'; // Set mock DATABASE_URL to pass config checks

// Override require cache for 'pg'
const pgMock = {
  Pool: function() {
    return {
      connect: async () => ({
        query: async (sql, values) => {
          return { rows: [] };
        },
        release: () => {}
      }),
      query: async (sql, values) => {
        if (sql.includes('INSERT INTO speed_history')) {
          return {
            rows: [{
              id: 99,
              user_id: values[0],
              user_name: values[1],
              group_name: values[2],
              max_speed: values[3],
              max_speed_10s: values[4],
              max_speed_500m: values[5],
              latitude: values[6],
              longitude: values[7],
              date: values[8],
              time: values[9],
              timestamp: values[10]
            }]
          };
        }
        if (sql.includes('COUNT(*)')) {
          return {
            rows: [{
              total_records: 12,
              highest_speed: 45.2,
              highest_speed_10s: 42.1,
              highest_speed_500m: 38.5,
              average_max_speed: 28.7
            }]
          };
        }
        if (sql.includes('SELECT * FROM speed_history')) {
          return {
            rows: [
              {
                id: 99,
                user_id: 'test_user_1',
                max_speed: 45.2,
                max_speed_10s: 42.1,
                max_speed_500m: 38.5,
                latitude: 10.1234,
                longitude: -20.5678,
                date: '2026-05-20',
                time: '18:00:00'
              }
            ]
          };
        }
        return { rows: [] };
      },
      on: () => {}
    };
  }
};
require.cache[require.resolve('pg')] = {
  id: require.resolve('pg'),
  filename: require.resolve('pg'),
  loaded: true,
  exports: pgMock
};

// Now load the server
require('../server.js');

test('Speed History API endpoints', async (t) => {
  // Wait a small moment for server to listen and db initialized
  await new Promise(resolve => setTimeout(resolve, 500));
  
  if (!serverInstance) {
    throw new Error('Server instance not captured');
  }

  const address = serverInstance.address();
  const port = address.port;
  const baseUrl = `http://localhost:${port}`;

  await t.test('POST /api/speed-history - stores a session record', async () => {
    const payload = {
      userId: 'test_user_1',
      userName: 'Carl',
      groupName: 'sailors',
      maxSpeed: 45.2,
      maxSpeed10s: 42.1,
      maxSpeed500m: 38.5,
      latitude: 10.1234,
      longitude: -20.5678,
      timestamp: Date.now()
    };

    const res = await fetch(`${baseUrl}/api/speed-history`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    assert.strictEqual(res.status, 201);
    const data = await res.json();
    assert.strictEqual(data.success, true);
    assert.strictEqual(data.record.user_id, 'test_user_1');
    assert.strictEqual(data.record.max_speed, 45.2);
  });

  await t.test('POST /api/speed-history - validates required fields', async () => {
    const payload = {
      userId: 'test_user_1'
      // missing maxSpeed, latitude, longitude, timestamp
    };

    const res = await fetch(`${baseUrl}/api/speed-history`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    assert.strictEqual(res.status, 400);
    const data = await res.json();
    assert.ok(data.error);
  });

  await t.test('GET /api/speed-history/:userId/stats - returns statistics for the user', async () => {
    const res = await fetch(`${baseUrl}/api/speed-history/test_user_1/stats`);
    assert.strictEqual(res.status, 200);
    const data = await res.json();
    assert.strictEqual(data.success, true);
    assert.strictEqual(data.statistics.total_records, 12);
    assert.strictEqual(data.statistics.highest_speed, 45.2);
  });

  await t.test('GET /api/speed-history/:userId - returns list of records', async () => {
    const res = await fetch(`${baseUrl}/api/speed-history/test_user_1`);
    assert.strictEqual(res.status, 200);
    const data = await res.json();
    assert.strictEqual(data.success, true);
    assert.ok(Array.isArray(data.records));
    assert.strictEqual(data.records.length, 1);
    assert.strictEqual(data.records[0].user_id, 'test_user_1');
  });

  // Clean up server
  serverInstance.close(() => {
    process.exit(0);
  });
});
