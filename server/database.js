// database.js - PostgreSQL database connection and schema
const { Pool } = require('pg');
require('dotenv').config();

// Create connection pool
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

// Test connection
pool.on('connect', () => {
  console.log('✅ Conectado a PostgreSQL');
});

pool.on('error', (err) => {
  console.error('❌ Error inesperado en PostgreSQL:', err);
});

// Initialize database schema
async function initializeDatabase() {
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    // Create speed_history table
    await client.query(`
      CREATE TABLE IF NOT EXISTS speed_history (
        id SERIAL PRIMARY KEY,
        user_id VARCHAR(255) NOT NULL,
        user_name VARCHAR(255),
        group_name VARCHAR(255),
        max_speed DECIMAL(10, 2) NOT NULL,
        latitude DECIMAL(10, 8) NOT NULL,
        longitude DECIMAL(11, 8) NOT NULL,
        date DATE NOT NULL,
        time TIME NOT NULL,
        timestamp BIGINT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Create index for faster queries by user_id
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_speed_history_user_id
      ON speed_history(user_id)
    `);

    // Create index for date queries
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_speed_history_date
      ON speed_history(date DESC)
    `);

    await client.query('COMMIT');
    console.log('✅ Esquema de base de datos inicializado correctamente');
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('❌ Error inicializando base de datos:', error);
    throw error;
  } finally {
    client.release();
  }
}

// Insert a new speed history record
async function insertSpeedHistory(data) {
  const { userId, userName, groupName, maxSpeed, latitude, longitude, date, time, timestamp } = data;

  const query = `
    INSERT INTO speed_history
    (user_id, user_name, group_name, max_speed, latitude, longitude, date, time, timestamp)
    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
    RETURNING *
  `;

  const values = [userId, userName, groupName, maxSpeed, latitude, longitude, date, time, timestamp];

  try {
    const result = await pool.query(query, values);
    return result.rows[0];
  } catch (error) {
    console.error('❌ Error insertando registro de velocidad:', error);
    throw error;
  }
}

// Get speed history for a specific user
async function getSpeedHistory(userId, limit = 100, offset = 0) {
  const query = `
    SELECT * FROM speed_history
    WHERE user_id = $1
    ORDER BY date DESC, time DESC
    LIMIT $2 OFFSET $3
  `;

  try {
    const result = await pool.query(query, [userId, limit, offset]);
    return result.rows;
  } catch (error) {
    console.error('❌ Error obteniendo historial de velocidad:', error);
    throw error;
  }
}

// Get all speed history records (admin endpoint)
async function getAllSpeedHistory(limit = 100, offset = 0) {
  const query = `
    SELECT * FROM speed_history
    ORDER BY date DESC, time DESC
    LIMIT $1 OFFSET $2
  `;

  try {
    const result = await pool.query(query, [limit, offset]);
    return result.rows;
  } catch (error) {
    console.error('❌ Error obteniendo todo el historial:', error);
    throw error;
  }
}

// Get speed history statistics for a user
async function getSpeedStatistics(userId) {
  const query = `
    SELECT
      COUNT(*) as total_records,
      MAX(max_speed) as highest_speed,
      AVG(max_speed) as average_max_speed,
      MIN(date) as first_record_date,
      MAX(date) as last_record_date
    FROM speed_history
    WHERE user_id = $1
  `;

  try {
    const result = await pool.query(query, [userId]);
    return result.rows[0];
  } catch (error) {
    console.error('❌ Error obteniendo estadísticas:', error);
    throw error;
  }
}

module.exports = {
  pool,
  initializeDatabase,
  insertSpeedHistory,
  getSpeedHistory,
  getAllSpeedHistory,
  getSpeedStatistics
};
