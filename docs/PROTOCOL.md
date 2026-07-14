# Protocol & Shared Behavior Reference

Single source of truth for the wire protocol, tuning constants, and cross-client
behavior shared by the four clients (web, Android phone, Wear OS, Garmin) and the
Node server. When you change a value here, change it in every implementation and
update this file. Implementations that must be kept in sync are noted per section.

Default production server: `wss://urban-ricca-cmontans-34613e13.koyeb.app`
(overridable per-client at runtime via in-app settings).

---

## 1. WebSocket protocol

All messages are JSON objects with a `type` field. Groups are the isolation
boundary: a client only receives messages for its own group.

### Group name normalization
The server normalizes every group name at every entry point (WebSocket, REST,
KML) identically: **trim → strip control characters → cap at 30 chars → lowercase**,
defaulting to `default` when empty. Group matching is therefore **case-insensitive**
(`Amigos` and `amigos` are the same group). Clients may display the name as the
user typed it, but must expect the server to treat it case-insensitively.

`userName` is capped at 20 chars, `userId` at 64, and control characters are
stripped server-side. Clients should apply the same input limits (the web/Android
name field uses `maxlength=20`, group `maxlength=30`).

### Client → server

| type | fields | notes |
|------|--------|-------|
| `register` | `userId`, `userName`, `groupName` | Join a group. Does **not** by itself place the user in the visible list — only `speed` does. |
| `speed` | `userId`, `userName`, `groupName`, `speed`, `maxSpeed`, `lat`, `lon`, `bearing`, `timestamp` | Position/speed update. `speed`/`maxSpeed` in km/h, `bearing` in degrees. Server drops the message if `userId` is empty or `lat`/`lon`/`speed` are non-finite. |
| `group-horn` | `userId`, `userName`, `groupName` | Audio alert to the group. Rate-limited to once per 5 s per user; server replies with `error` when rate-limited. |
| `group-jump` | `userId`, `userName`, `groupName`, `maxHeight`, `hangtime` | Kitesurf jump event. `maxHeight` in meters, `hangtime` in ms. |
| `ping` | — | Keep-alive. Server replies `pong`. Send every **25 s**. |

There is **no `join` message.** Viewer/visualizer mode is implemented purely
client-side as "`register` but never send `speed`" — such a connection never
appears in the users map. (An older `join` message existed server-side but was
dead code and has been removed.)

### Server → client

| type | fields | notes |
|------|--------|-------|
| `users` | `users: UserData[]` | Full list of active users in the group. Coalesced and broadcast at **1 Hz** per group (not once per incoming `speed`). |
| `pong` | — | Reply to `ping`. |
| `group-horn` | `userId`, `userName`, `groupName`, `timestamp` | Relayed horn; clients play the sound unless they sent it. |
| `group-jump` | `userId`, `userName`, `groupName`, `maxHeight`, `hangtime`, `timestamp` | Relayed jump event. |
| `error` | `message` | Human-readable error (e.g. horn rate-limit). |

`UserData` = `{ userId, userName, speed, maxSpeed, lat, lon, bearing, timestamp }`.
(The server also tracks `receivedAt` internally for liveness but does not send it.)

### Server-side liveness & timing
- **Idle eviction:** a user is removed after **10 s** without a `speed` update,
  measured by the server-side receipt time (`receivedAt`), **not** the client
  `timestamp` — so a device with a skewed clock is neither evicted instantly nor
  kept forever. Empty groups are deleted.
- **Broadcast coalescing:** `speed` updates mark the group dirty; a 1 Hz timer
  flushes one combined `users` broadcast per group (avoids the O(N²) storm of
  re-broadcasting on every per-user message).
- **Per-connection rate limit:** at most 30 messages/second per socket; excess is
  dropped silently.
- **Horn cooldown:** 5 s per user, swept from memory as it expires.

---

## 2. REST API

Base URL is the server origin over HTTPS (clients convert `wss://`→`https://`,
stripping any trailing slash so paths never become `//api/...`).

- `POST /api/speed-history` — persist one completed-session record. Body:
  `{ userId, userName, groupName, maxSpeed, maxSpeed10s, maxSpeed500m, latitude, longitude, timestamp }`.
  **Submit exactly once, on stop.** (Mid-session submission is forbidden — it
  creates duplicate session rows and inflates aggregate statistics.)
- `GET /api/speed-history/:userId?limit=&offset=` — a user's records.
- `GET /api/speed-history/:userId/stats` — aggregate stats for a user.
- `GET /api/speed-history` — all records (admin).
- `GET/POST/PUT/DELETE /api/waypoints[...]` — group-scoped waypoint CRUD. Group
  names are normalized (lowercased) the same way as the WebSocket path.
- `GET /groups`, `GET /groups/:groupName` — in-memory group snapshots.
- `GET /kml/network-link`, `GET /kml/users` — live KML feed for Google Earth. All
  user-controlled fields are XML-escaped. See `KML_NETWORK_LINK.md`.

---

## 3. Speed & GPS constants

Kept in sync across: web `GPS_CONFIG` (`netlify/index.html`), Android
`Constants.kt` (`shared/util`), and the Garmin/web equivalents.

| Constant | Value | Meaning |
|----------|-------|---------|
| `MS_TO_KMH` | 3.6 | m/s → km/h |
| `KMH_TO_MPH` | 0.621371 | km/h → mph |
| `KMH_TO_KNOTS` | 0.539957 | km/h → knots |
| `MAX_GPS_ACCURACY` | 10.0 m | reject fixes worse than this (web + phone) |
| `WEAR_MAX_GPS_ACCURACY` | 25.0 m | relaxed threshold for the watch |
| `MIN_SPEED_THRESHOLD` | 1.5 km/h | speeds below this are treated as stationary (0) |
| `MAX_SPEED_READINGS` | 20 | rolling-average window size |
| `AVG_SPEED_TIME_WINDOW` | 10 000 ms | "10 s" peak/average window |
| `AVG_SPEED_DISTANCE_WINDOW` | 500 m | "500 m" peak/average window |
| keep-alive ping interval | 25 s | client → server `ping` |

### Speed derivation (all clients)
Use the hardware-reported speed when the fix provides one; otherwise fall back to
`distance / elapsed_time` between consecutive accepted fixes (haversine distance).
The rolling **avg / 10 s / 500 m** statistics use the thresholded speed, while the
session **max** uses the raw (pre-threshold) speed. Android's implementation lives
in the shared, unit-tested `SpeedCalculator` and `SessionSpeedStats`; the web
client mirrors the same math inline.

---

## 4. Jump detection algorithm (kitesurf)

Three parallel implementations must stay behaviorally equivalent — there is no
shared source code across languages:
- `android/shared/util/JumpDetector.kt` + `AltitudeKalmanFilter.kt` (Kotlin, reference)
- `garmin/source/JumpDetector.mc` + `AltitudeKalmanFilter.mc` (Monkey C)
- `kalman_test.py` / `kalman_test.js` (experimentation only)

### Tuned constants (reference values)

| Constant | Value | Notes |
|----------|-------|-------|
| Kalman `processNoise` | 0.05 | smoothness of altitude changes |
| Kalman `measurementNoise` | 2.5 | barometer jitter damping |
| Adaptive-noise residual gate | 3.0 m | residuals above this ×20 the measurement noise (rejects "wetsuit squeeze" spikes) |
| Takeoff threshold | 22 m/s² | on **net (gravity-removed)** vertical/impact acceleration (sensitivity MEDIA) |
| Landing threshold | 15 m/s² | net impact spike after ≥ 800 ms airborne |
| Min hangtime | 1500 ms | shorter airtime is discarded |
| Jump cooldown | 2000 ms | minimum gap between jumps |
| Safety timeout | 10 000 ms | force-complete a stuck jump |

Sensitivity presets (Android): `BAJA(30.0, minHeight 1.5)`,
`MEDIA(22.0, minHeight 1.0)`, `ALTA(15.0, minHeight 0.5)`. Garmin currently
uses MEDIA only.

### Platform note on acceleration
Android uses `TYPE_LINEAR_ACCELERATION` (gravity already removed) projected onto
the world vertical axis. Garmin's `Sensor.Info.accel` **includes gravity**, so the
Garmin detector subtracts 1 g (9.81 m/s²) from the magnitude before comparing to
the thresholds. This is an approximation of Android's rotation-projected vertical
acceleration; the numeric thresholds (22 / 15) are shared. The baseline altitude
at takeoff is captured from the filter's current smoothed altitude
(`getCurrentAltitude()`) — never by feeding 0.0 into the filter.

---

## 5. Garmin ↔ server bridge (status)

The Garmin app broadcasts speed/jump events over Bluetooth via
`Communications.transmit()` (`CommListener.mc`), but the Android app does not yet
have a Connect IQ receiver, so Garmin data does **not** currently reach the server
or the group. Closing this loop requires wrapping the Connect IQ Android SDK
`.aar` into `android/app` — see `android/GARMIN_INTEGRATION.md`. Until then the
Garmin app is effectively standalone.
