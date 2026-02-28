# Koyeb Deployment Guide

This guide covers deploying the GPS Tracker server to [Koyeb](https://www.koyeb.com) using Docker, including setting up a PostgreSQL database for persistent speed history and waypoints.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture Overview](#architecture-overview)
3. [Database Setup](#database-setup)
   - [Option A: Koyeb Managed PostgreSQL](#option-a-koyeb-managed-postgresql)
   - [Option B: External PostgreSQL Provider](#option-b-external-postgresql-provider)
4. [Deploy the Server on Koyeb](#deploy-the-server-on-koyeb)
   - [Option 1: Deploy from GitHub](#option-1-deploy-from-github)
   - [Option 2: Deploy from Docker Image](#option-2-deploy-from-docker-image)
5. [Environment Variables](#environment-variables)
6. [Verify the Deployment](#verify-the-deployment)
7. [Connect the Web Client](#connect-the-web-client)
8. [API Reference](#api-reference)
9. [Database Schema](#database-schema)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- A [Koyeb account](https://app.koyeb.com/auth/signup) (free tier available)
- This repository pushed to GitHub (or a Docker registry)
- Basic familiarity with environment variables

---

## Architecture Overview

```
┌──────────────────────┐        WebSocket / HTTP
│   Web Client (PWA)   │ ──────────────────────────► ┌─────────────────────┐
│   (Netlify / CDN)    │                              │  GPS Tracker Server  │
└──────────────────────┘                              │  (Koyeb - Docker)    │
                                                      └──────────┬──────────┘
┌──────────────────────┐                                         │
│  Android App         │ ──────────────────────────►             │ PostgreSQL
│  (Native / APK)      │                              ┌──────────▼──────────┐
└──────────────────────┘                              │  PostgreSQL Database  │
                                                      │  (Koyeb or external) │
                                                      └─────────────────────┘
```

The Node.js server runs as a Docker container on Koyeb, handling:
- WebSocket connections for real-time GPS tracking
- REST API for speed history and waypoints
- PostgreSQL for persistent storage

---

## Database Setup

The server stores speed history records and group waypoints in PostgreSQL. The schema is created automatically on first startup — no manual migration needed.

### Option A: Koyeb Managed PostgreSQL

Koyeb offers managed PostgreSQL databases directly in the dashboard.

1. Log in to the [Koyeb Control Panel](https://app.koyeb.com)
2. Navigate to **Databases** in the left sidebar
3. Click **Create Database Service**
4. Configure:
   - **Name**: `gps-tracker-db` (or any name you prefer)
   - **Region**: Choose the same region you'll use for the server (lower latency)
   - **Plan**: Free tier is sufficient for most use cases
5. Click **Create Database**
6. Once created, go to the database **Overview** tab and copy the **Connection string**

The connection string will look like:
```
postgresql://koyeb-adm:<password>@<host>.koyeb.app:5432/<db-name>?sslmode=require
```

> **Note**: Keep this connection string — you'll need it when configuring environment variables for the server.

---

### Option B: External PostgreSQL Provider

If you prefer using a third-party provider, here are the most common options:

#### Neon (Recommended for free tier)

1. Create a free account at [neon.tech](https://neon.tech)
2. Create a new project and select a region close to your Koyeb server
3. In the **Dashboard**, click **Connection Details**
4. Copy the **Connection string** (pooled connection recommended)

Format:
```
postgresql://user:password@ep-<id>.us-east-2.aws.neon.tech/neondb?sslmode=require
```

#### Railway

1. Create a project at [railway.app](https://railway.app)
2. Click **New** → **Database** → **Add PostgreSQL**
3. Go to the PostgreSQL service → **Connect** tab
4. Copy the **DATABASE_URL**

Format:
```
postgresql://postgres:<password>@<host>.railway.app:5432/railway
```

#### Supabase

1. Create a project at [supabase.com](https://supabase.com)
2. Go to **Settings** → **Database**
3. Under **Connection string**, select **URI** and copy it

Format:
```
postgresql://postgres:<password>@db.<project-ref>.supabase.co:5432/postgres
```

> **SSL Note**: The server automatically enables SSL when `DATABASE_URL` is set. All major cloud providers require SSL and the server is configured to handle this correctly with `rejectUnauthorized: false` to support self-signed certificates common in managed databases.

---

## Deploy the Server on Koyeb

### Option 1: Deploy from GitHub

This is the simplest method. Koyeb builds the Docker image automatically from your repository.

**Step 1: Push your code to GitHub**

Ensure your repository is pushed to GitHub. The `Dockerfile` at the project root is already configured for Koyeb.

**Step 2: Create a Koyeb Service**

1. In the Koyeb Control Panel, click **Create Service**
2. Select **GitHub** as the deployment method
3. Connect your GitHub account if not already done
4. Select your repository (`gps-tracker-client`) and the branch (e.g., `master`)
5. Koyeb will detect the `Dockerfile` automatically

**Step 3: Configure the Build**

- **Builder**: Docker (auto-detected)
- **Dockerfile location**: `./Dockerfile` (root of the repo)
- **Build context**: `.` (root)

**Step 4: Configure the Service**

- **Name**: `gps-tracker-server`
- **Region**: Choose a region (e.g., `was` for Washington DC, `fra` for Frankfurt)
- **Instance type**: Free (Nano) is sufficient for small groups; upgrade for more users
- **Ports**: Set to `8080` with protocol `HTTP`

**Step 5: Set Environment Variables**

In the **Environment variables** section, add:

| Variable | Value |
|----------|-------|
| `DATABASE_URL` | Your PostgreSQL connection string |
| `NODE_ENV` | `production` |

> **Security**: Use Koyeb **Secrets** for `DATABASE_URL` instead of plain environment variables. In the Koyeb dashboard, go to **Secrets** → **Create Secret**, then reference it as `@your-secret-name` in the environment variable value.

**Step 6: Deploy**

Click **Deploy**. Koyeb will:
1. Clone your repository
2. Build the Docker image
3. Run the container
4. Assign a public URL (e.g., `https://gps-tracker-server-<hash>.koyeb.app`)

---

### Option 2: Deploy from Docker Image

If you prefer to push a pre-built image to a Docker registry:

**Step 1: Build the image locally**

```bash
# From the repository root
docker build -t your-dockerhub-username/gps-tracker-server:latest .

# Test locally (optional)
docker run -p 8080:8080 \
  -e DATABASE_URL="postgresql://user:pass@host:5432/db" \
  -e NODE_ENV=production \
  your-dockerhub-username/gps-tracker-server:latest
```

**Step 2: Push to Docker Hub**

```bash
docker login
docker push your-dockerhub-username/gps-tracker-server:latest
```

**Step 3: Create Koyeb Service**

1. In Koyeb, click **Create Service**
2. Select **Docker** as the deployment method
3. Enter the image: `your-dockerhub-username/gps-tracker-server:latest`
4. Configure ports and environment variables as in Option 1 Step 4 and 5

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | — | PostgreSQL connection string. Without this, the server starts but speed history and waypoints won't persist across restarts. |
| `NODE_ENV` | No | `development` | Set to `production` for optimized performance and reduced debug logging. |
| `PORT` | No | `3001` | HTTP/WebSocket port. **Koyeb sets this automatically** — do not override. |

### Connection String Format

```
postgresql://username:password@host:port/database?sslmode=require
```

**Examples:**

```bash
# Koyeb Managed PostgreSQL
DATABASE_URL=postgresql://koyeb-adm:abc123@ep-quiet-leaf.koyeb.app:5432/koyebdb?sslmode=require

# Neon
DATABASE_URL=postgresql://myuser:abc123@ep-cool-lab-123.us-east-2.aws.neon.tech/neondb?sslmode=require

# Railway
DATABASE_URL=postgresql://postgres:abc123@roundhouse.proxy.rlwy.net:12345/railway

# Supabase
DATABASE_URL=postgresql://postgres:abc123@db.abcxyz.supabase.co:5432/postgres
```

---

## Verify the Deployment

Once deployed, Koyeb assigns a public URL like:
```
https://gps-tracker-server-<random>.koyeb.app
```

**1. Check the health endpoint**

```bash
curl https://gps-tracker-server-<random>.koyeb.app/health
```

Expected response:
```json
{
  "status": "ok",
  "users": 0,
  "groups": 0
}
```

**2. Check active groups**

```bash
curl https://gps-tracker-server-<random>.koyeb.app/groups
```

**3. Check database connectivity**

Look at the Koyeb service logs (Control Panel → Service → **Logs** tab). On successful startup you should see:

```
✅ Conectado a PostgreSQL
✅ Esquema de base de datos inicializado correctamente
Server listening on port 8080
```

If the database connection fails, the server will still start but log an error. Speed history and waypoints will not be saved in that case.

**4. Test the WebSocket connection**

Open your browser console and run:

```javascript
const ws = new WebSocket('wss://gps-tracker-server-<random>.koyeb.app');
ws.onopen = () => console.log('Connected!');
ws.onmessage = (e) => console.log('Message:', e.data);
```

---

## Connect the Web Client

After deploying, update the web client to point to your new Koyeb server.

### Option A: Update the default server URL in `netlify/index.html`

Search for the default WebSocket URL in the HTML file and replace it with your Koyeb URL:

```javascript
// Change this line
const DEFAULT_SERVER = 'wss://old-server.railway.app';

// To your Koyeb URL
const DEFAULT_SERVER = 'wss://gps-tracker-server-<random>.koyeb.app';
```

### Option B: Use the in-app settings

Users can configure a custom server URL without changing the source code:

1. Open the web app
2. Click **⚙️ Ajustes** (Settings)
3. Enter your Koyeb WebSocket URL:
   ```
   wss://gps-tracker-server-<random>.koyeb.app
   ```
4. Click **Save** — the app will reconnect to the new server

> **HTTPS/WSS**: Koyeb automatically provides TLS for all services. Always use `wss://` (not `ws://`) for secure WebSocket connections. The `https://` URL is used for REST API calls.

---

## API Reference

All endpoints are relative to your Koyeb service URL.

### Health & Status

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Server health, active user count, active group count |
| `GET` | `/groups` | List all active groups and their members |
| `GET` | `/groups/:groupName` | List members of a specific group |

### Speed History

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/speed-history` | Save a speed record |
| `GET` | `/api/speed-history/:userId` | Get speed history for a user |
| `GET` | `/api/speed-history/:userId/stats` | Get speed statistics for a user |
| `GET` | `/api/speed-history` | Get all speed history records |

**POST `/api/speed-history` body:**
```json
{
  "userId": "abc123",
  "userName": "John",
  "groupName": "TeamA",
  "maxSpeed": 87.4,
  "latitude": 40.712776,
  "longitude": -74.005974,
  "date": "2024-01-15",
  "time": "14:30:00",
  "timestamp": 1705329000000
}
```

### Waypoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/waypoints` | Create a waypoint |
| `GET` | `/api/waypoints/:groupName` | Get all waypoints for a group |
| `PUT` | `/api/waypoints/:id` | Update a waypoint |
| `DELETE` | `/api/waypoints/:id` | Delete a waypoint |

### KML / Google Earth

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/kml/network-link` | Auto-refreshing KML NetworkLink for Google Earth |
| `GET` | `/kml/users` | Current user positions as KML |

**KML query parameters:**
- `?group=TeamA` — Filter to a specific group
- `?refresh=10` — Set auto-refresh interval in seconds (default: 5)

### WebSocket Protocol

Connect to `wss://your-server.koyeb.app` and send JSON messages:

| Message Type | Direction | Description |
|-------------|-----------|-------------|
| `register` | Client → Server | Register user with `userId`, `userName`, `groupName` |
| `speed` | Client → Server | Send GPS update with position, speed, bearing, timestamp |
| `users` | Server → Client | Broadcast of all active group members |
| `group-horn` | Client ↔ Server | Send/receive group horn alert |
| `join` | Client → Server | Join as viewer-only (no tracking) |
| `ping` / `pong` | Both | Keep-alive heartbeat (every 25 seconds) |

---

## Database Schema

The schema is created automatically on first server startup. No manual setup or migration commands are needed.

### `speed_history` Table

Stores maximum speed records saved by users during tracking sessions.

```sql
CREATE TABLE speed_history (
  id           SERIAL PRIMARY KEY,
  user_id      VARCHAR(255) NOT NULL,
  user_name    VARCHAR(255),
  group_name   VARCHAR(255),
  max_speed    DECIMAL(10, 2) NOT NULL,    -- km/h
  latitude     DECIMAL(10, 8) NOT NULL,
  longitude    DECIMAL(11, 8) NOT NULL,
  date         DATE NOT NULL,
  time         TIME NOT NULL,
  timestamp    BIGINT NOT NULL,            -- Unix timestamp (ms)
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_speed_history_user_id ON speed_history(user_id);
CREATE INDEX idx_speed_history_date    ON speed_history(date DESC);
```

### `waypoints` Table

Stores named map waypoints shared within a group.

```sql
CREATE TABLE waypoints (
  id           SERIAL PRIMARY KEY,
  group_name   VARCHAR(255) NOT NULL,
  name         VARCHAR(255) NOT NULL,
  description  TEXT,
  latitude     DECIMAL(10, 8) NOT NULL,
  longitude    DECIMAL(11, 8) NOT NULL,
  created_by   VARCHAR(255),
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index
CREATE INDEX idx_waypoints_group_name ON waypoints(group_name);
```

> The server uses `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`, so re-deploying will never drop or recreate existing tables or data.

---

## Troubleshooting

### Service fails to start

**Check the logs** in the Koyeb Control Panel → Service → **Logs**.

Common causes:
- Missing `DATABASE_URL` — the server logs a clear error if the DB connection fails
- Wrong port — Koyeb sets `$PORT` automatically; never hardcode a port in env vars

### Database connection error

```
❌ Error inesperado en PostgreSQL: ...
```

1. Verify the `DATABASE_URL` is correct and the database exists
2. Ensure the database allows connections from external IPs (some providers require adding `0.0.0.0/0` to the allowed IP list)
3. Check that SSL mode matches what your provider requires (`?sslmode=require` for most cloud providers)
4. The server will continue running without a database — only persistence is affected

### WebSocket connection refused

- Ensure you're using `wss://` not `ws://` (Koyeb enforces HTTPS/WSS)
- Verify the service is in **Running** state (not **Deploying** or **Error**)
- Check that port `8080` is configured in Koyeb service settings

### Speed history not saving

- Check that `DATABASE_URL` is set and the server logs show `✅ Conectado a PostgreSQL`
- Query the health endpoint — if the server is up but DB is not connected, history calls will fail with a 500 error
- Check the database's connection limit; free-tier databases often cap at 5–10 concurrent connections

### High latency / connection drops

- Choose a Koyeb region geographically close to your users
- The server sends keep-alive pings every 25 seconds; clients auto-reconnect with exponential backoff (up to 10 attempts, max 30s delay)
- Upgrade to a larger Koyeb instance if you have many concurrent users

### "Cold start" delays (free tier)

Koyeb free-tier services may spin down after inactivity. The first request after a cold start may take a few seconds. Consider:
- Upgrading to a paid instance for always-on availability
- Setting up an external uptime monitor (e.g., UptimeRobot) to ping `/health` every 5 minutes

---

## Updating the Deployment

### GitHub-based deployment (automatic)

If you connected Koyeb to GitHub, every push to the configured branch triggers an automatic rebuild and redeploy with zero downtime.

### Docker-based deployment (manual)

```bash
# Rebuild and push the image
docker build -t your-dockerhub-username/gps-tracker-server:latest .
docker push your-dockerhub-username/gps-tracker-server:latest

# Then in the Koyeb dashboard, click "Redeploy" on your service
# or trigger via Koyeb CLI:
koyeb service redeploy gps-tracker-server
```
