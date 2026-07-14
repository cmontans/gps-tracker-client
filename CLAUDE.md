# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Multi-platform real-time GPS speed/jump tracking system with four independent clients sharing one WebSocket protocol and one backend:

- `netlify/` — Vanilla JS/HTML/CSS web app (PWA), deployed to Netlify. `index.html` is a single monolithic file containing all HTML, CSS, and JS (~3000 lines). `waypoints.html` is a separate standalone page.
- `android/` — Native Android app (Kotlin), a multi-module Gradle project: `app` (phone UI/foreground service), `wear` (Wear OS companion), `shared` (code shared between `app` and `wear`).
- `garmin/` — Garmin Connect IQ (Monkey C) companion app, mirrors the Wear OS app's speed tracking and kitesurf jump detection/history.
- `server/` — Node.js WebSocket + REST backend (Express + `ws` + PostgreSQL), deployed via Docker to Koyeb.

All clients speak the same WebSocket JSON protocol to the same server and can interoperate live (e.g. a phone and a Garmin watch in the same group see each other).

## Commands

### Server (`server/`)
```bash
cd server
npm install
npm start                 # runs server.js on $PORT (default 3001)
node --test                # run server tests (Node built-in test runner; auto-discovers test/)
node --test test/server.test.js
```
Requires `DATABASE_URL` env var (PostgreSQL, SSL always on for cloud DBs). See `server/.env.example`. Without it, the server still runs but persistence (`speed_history`, `waypoints` tables) is disabled — `db.initializeDatabase()` failure is caught and logged, not fatal.

### Web client (`netlify/`)
No build step — served as static files. Tests use Node's built-in test runner against the raw HTML:
```bash
node --test netlify/test/frontend.test.js
```
Tests work by regex-extracting named function bodies out of `index.html`'s inline `<script>` (see `extractFunction` in `netlify/test/frontend.test.js`) and evaluating them standalone — when renaming or restructuring a tested function in `index.html`, keep it as a top-level `function name(...) { ... }` declaration or the extraction breaks.

### Android (`android/`)
```bash
cd android
./gradlew build
./gradlew installDebug
./gradlew assembleRelease
./gradlew test                                    # unit tests, all modules
./gradlew :app:testDebugUnitTest --tests "*AltitudeKalmanFilterTest*"
```
Requires a Google Maps API key in `android/app/src/main/AndroidManifest.xml` (`com.google.android.geo.API_KEY`) to build/run map features.

### Garmin (`garmin/`)
Built and run via the Monkey C VS Code extension (no CLI build documented) — open the `garmin/` folder directly in VS Code (not the repo root) so the extension detects `monkey.jungle`/`manifest.xml`. Commands: `Monkey C: Build and Run` (simulator), `Monkey C: Build For Device` (produces `.prg` in `garmin/bin/`).

### Root-level scratch scripts
`kalman_test.js` / `kalman_test.py` at repo root are standalone experiment scripts for the altitude Kalman filter (not part of any test suite / not wired to a runner).

## Architecture

### WebSocket protocol (shared across all clients and the server)
**See `docs/PROTOCOL.md` for the authoritative message schemas, tuning constants, and jump-algorithm reference — keep it in sync when changing behavior.**

Groups are the isolation boundary: users only see others in the same `groupName` (case-insensitive, normalized to lowercase at every server entry point). Message `type` values, handled in `server/server.js`:
- `register` — client joins with `userId`, `userName`, `groupName`; creates the group if needed and triggers a `users` broadcast. (Does not by itself add the user to the visible list — only `speed` does.)
- `speed` — position/speed/bearing update; server tracks running `maxSpeed` per user in-memory, stamps a server-side `receivedAt`, and marks the group dirty for the next broadcast.
- `ping` / `pong` — keep-alive heartbeat (all clients ping every 25s, including Android).
- `group-horn` — audio/notification alert to the group; server enforces a 5s per-user cooldown (`hornRateLimit` map) and replies with an `error` message if rate-limited.
- `group-jump` — kitesurf jump event (height/hangtime) broadcast to the group, added for the Wear/Garmin jump-tracking feature.

Viewer/visualizer mode is client-side only ("register but never send `speed`"); there is no `join` message. `users` broadcasts are coalesced to 1 Hz per group (a dirty-set flushed on an interval) rather than sent per incoming `speed`.

Server state is in-memory only (`groups: Map<groupName, Map<userId, userData>>`); a background interval (every 5s) evicts users idle >10s (by server-side `receivedAt`) and deletes empty groups. Nothing here is persisted — persistence is a separate, explicit path.

### Server persistence (`server/database.js`)
PostgreSQL is used only for two durable records, both written via explicit REST calls (not automatically from WebSocket traffic):
- `speed_history` — one row per completed tracking session (max/10s/500m speeds, final position), inserted via `POST /api/speed-history`. Schema migrations for added columns run idempotently inside `initializeDatabase()` (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`-style guard using `information_schema`).
- `waypoints` — user-created map markers scoped to a group, full CRUD under `/api/waypoints`.

The server also exposes a live KML feed (`/kml/network-link`, `/kml/users`) that renders the in-memory `groups` state as KML placemarks for Google Earth/Maps, filtered to users updated in the last 30s — see `KML_NETWORK_LINK.md`.

### Android module boundaries
- `shared/` holds code used by both the phone (`app`) and the watch (`wear`): WebSocket message models (`shared/model/WebSocketMessages.kt`), the Room DB for track/jump storage (`shared/db/`), `AltitudeKalmanFilter`, `JumpDetector`, and `DataSerializer` (used to pass data over the Wearable Data Layer API between phone and watch).
- `app/` is the phone client: `LocationTrackingService` (foreground service) + `WebSocketClient` talk to the server directly; `WearableDataService` / `PhoneDataLayerListenerService` relay data to/from the paired watch via the Data Layer API; `JumpTrackingService` handles kitesurf jump detection on-phone.
- `wear/` is the Wear OS companion: `WearLocationService` + `DataLayerListenerService` mirror tracking locally and sync with the phone; it does not talk to the WebSocket server directly (relies on the phone for that — see `garmin/README.md`'s note on Connect IQ lacking long-lived WebSocket support, which is why the Garmin app instead relies on `CommListener.mc` + Bluetooth `Communications.transmit()` to reach the phone, unlike Wear OS which can use Data Layer sync).
- `JumpDetector` / `AltitudeKalmanFilter` logic exists in three parallel implementations: `shared/util/` (Android, Kotlin, the reference), `garmin/source/*.mc` (Monkey C), and the root `kalman_test.py`/`kalman_test.js` (experimentation only). There is no shared source code across languages — `docs/PROTOCOL.md` §4 holds the tuned constants; keep all three in sync when changing the algorithm. Note Garmin subtracts gravity from its raw accelerometer magnitude to approximate Android's gravity-removed vertical acceleration.
- Android speed math is extracted into shared, unit-tested helpers: `SessionSpeedStats` (rolling avg / 10s / 500m + peaks) and `SpeedCalculator` (hardware-speed with distance/time fallback). The web client mirrors the same math inline in `index.html`.

### Default production server
All three clients (web, Android, Garmin via phone bridge) default to `wss://urban-ricca-cmontans-34613e13.koyeb.app`; overridable per-client at runtime via in-app settings, not a build-time config.
