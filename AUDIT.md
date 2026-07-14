# Full Audit — GPS Tracker (Web / Android / Wear OS / Garmin / Server)

Date: 2026-07-14. Scope: correctness bugs, security, cross-platform standardization, and a UI modernization plan (Surfr-style). File references use `path:line` of the current `main`.

Test status at audit time: `netlify` 3/3 pass, `server` 3/3 pass, coverage is minimal (6 tests total for ~8k LOC). No CI is configured.

---

## 1. Critical — security

### 1.1 Stored/reflected XSS in the web client (HIGH)
`netlify/index.html` builds HTML with template literals + `innerHTML` (30 usages) using **data received from other users**:
- `updateUsersList()` (index.html:2561–2596) injects `user.userName` and `user.userId` unescaped.
- Map marker labels/popups (`createArrowIcon`, `updateMap`) inject `userName`.
- `waypoints.html:291` renders waypoint `name`/`description` from the REST API unescaped — waypoints are group-shared and anyone can POST them.

Any group member (or anyone who guesses a group name — there is no auth) can set their `userName` to `<img src=x onerror=...>` and execute JS on every group member's device. **Fix:** a single `escapeHtml()` helper applied at every interpolation site, or build nodes with `textContent`. The server should also enforce length + character limits (see 1.4).

### 1.2 XML injection in KML feed (MEDIUM)
`server/server.js:608–629` (`generateUsersKML`) interpolates `user.userName`, `userId`, `groupName` into KML/XML without escaping. A malicious name breaks the document or injects elements into Google Earth clients. **Fix:** escape `& < > " '` before interpolation.

### 1.3 TLS certificate validation disabled (MEDIUM)
`server/database.js:18` — `ssl: { rejectUnauthorized: false }` accepts any certificate (MITM on the DB connection). **Fix:** use the provider CA bundle, or gate the relaxed mode behind an env flag.

### 1.4 No validation, auth, or rate limits on the REST/WS surface (MEDIUM)
- WS `register`/`speed` accept unbounded `userName`/`groupName` strings into in-memory maps (server.js:96–155) — memory abuse and no charset limits (feeds 1.1/1.2).
- `POST /api/speed-history` accepts any `userId` — anyone can pollute anyone's history; `GET /api/speed-history` ("admin") is public.
- Waypoints CRUD is unauthenticated including DELETE by numeric id (easily enumerable across groups).
- Only the horn has a rate limit; `speed` messages have none.

**Fix (pragmatic for a hobby deployment):** cap `userName` ≤ 20 / `groupName` ≤ 30 chars server-side (mirroring client `maxlength`), sanitize charset, per-connection message rate cap, shared-secret header for the admin endpoint, and scope waypoint update/delete by `groupName`.

---

## 2. Critical — correctness bugs

### 2.1 Android 500 m average is broken (HIGH)
`LocationTrackingService.kt`: `lastLocation = location` is assigned at line 220, *then* line 262 computes `last.distanceTo(location)` — measuring the distance of the location **to itself**. `dist` is always 0, `totalDistance` never grows, `windowDist > 50` never fires ⇒ `avg500m`/`max500m` are permanently 0.0 on Android (web computes them correctly). **Fix:** capture `previousLocation` before reassigning, or move the assignment to the end of `handleLocationUpdate`.

### 2.2 Android still submits duplicate speed-history records (HIGH)
Commit `a9cb66a` removed periodic mid-session submission from the web client to stop duplicate session rows — but Android still has it: `LocationTrackingService.kt:307–314` submits every time max speed rises (60 s throttle) *and* again in `stopTracking()` (line 134). One session ⇒ several DB rows, inflating `total_records`/`average_max_speed` stats. **Fix:** submit once on stop, same as web.

### 2.3 Garmin jump detection is not physically equivalent to Android (HIGH for feature parity)
- Android (`shared/util/JumpDetector.kt:100–104`) uses **gravity-removed, rotation-compensated vertical** acceleration with a 22 m/s² takeoff threshold. Garmin (`garmin/source/JumpDetector.mc:65–69`) uses **raw accelerometer magnitude, which includes gravity (~9.8 m/s²)**, against the same 22 threshold — the two fire under very different conditions.
- `JumpDetector.mc:72`: if `info.altitude` is null at takeoff, `kalmanFilter.update(0.0f)` is called — feeding altitude 0 into the filter corrupts the baseline and subsequent height numbers.
- Kalman parameters diverge: Kotlin defaults `(0.05, 2.5)` (`AltitudeKalmanFilter.kt:8–9`), Garmin instantiates `(0.01, 2.0)` (`JumpDetector.mc:25`). Kotlin also has the `reset()`-on-init + `getCurrentAltitude()` used for baseline; Garmin re-updates the filter instead.
- Garmin sensitivity is hardcoded to MEDIA; Android exposes BAJA/MEDIA/ALTA.

**Fix:** subtract gravity on Garmin (or use `info.accel` minus 9.81 on magnitude), guard the null-altitude path, and centralize the tuned constants in one documented table that all three implementations copy from (CLAUDE.md already warns there is no shared source of truth — give it one reference file, e.g. `docs/JUMP_ALGORITHM.md`).

### 2.4 Server trusts client clocks for liveness (MEDIUM)
`server.js:64–85` evicts users when `now - user.timestamp > 10000`, but `timestamp` is the **client-supplied** `Date.now()` (`server.js:148`). A device with a skewed clock is evicted instantly or never. The KML 30 s freshness filter (line 605) has the same flaw. **Fix:** stamp `receivedAt: Date.now()` server-side and evict on that.

### 2.5 Eviction broadcast flag never resets per group (LOW)
`server.js:66` — `hasChanges` is declared outside `groups.forEach`, so once any group has a change, every later group in the iteration re-broadcasts its user list too. Cosmetic traffic bug; move the flag inside the loop.

### 2.6 Group-name case handling is inconsistent (MEDIUM)
- WS handlers lowercase `groupName`; REST does not: `GET /groups/:groupName` (server.js:317–319) and `GET /kml/users?group=` (server.js:695) do exact-case `Map.get` against lowercased keys ⇒ 404/empty for `?group=Amigos`.
- Waypoints REST stores/queries `group_name` **without** normalization (`database.js:200–236`), so web (sends as typed) and Android may see different waypoint sets for the "same" group.
- Both READMEs and the web helper text say groups are **case-sensitive**; the server has been case-insensitive since normalization was added. Docs and REST behavior must be aligned — recommend: normalize to lowercase at every entry point (WS, REST, KML) and fix the docs.

### 2.7 `hornRateLimit` map leaks (LOW)
Entries are only deleted on clean WS close with a `userId` (server.js:255). Unclean disconnects and REST-only users accumulate forever. Sweep it in the existing 5 s cleanup interval.

---

## 3. Standardization gaps across deployments

| Concern | Web | Android phone | Wear OS | Garmin |
|---|---|---|---|---|
| Keep-alive ping (25 s) | ✅ | ❌ none | n/a (via phone) | n/a |
| Speed fallback when GPS speed is null (distance/Δt) | ✅ | ❌ reports 0 | ❌ | ❌ |
| GPS accuracy gate | 10 m | 10 m | 25 m | none |
| Viewer mode | `register` + suppressed sends | same | n/a | n/a |
| Session history submit | on stop only | on stop **and** periodic (bug 2.2) | via phone | ❌ |
| Waypoints | ✅ | ✅ | ❌ | ❌ |
| Jump detection/display | display only (toast) | detect + display | detect + display | detect + display (isolated) |
| FIT export | ✅ | ✅ | ❌ | ❌ |
| i18n | es/en/fr/de | res for en/es/de/fr but bypassed in places | — | en only |

Key items:

- **3.1 Android has no keep-alive.** `GPSWebSocketClient.kt` replies to a server `ping` that the server never sends, and never initiates the 25 s ping the web client sends. During GPS-quiet periods (accuracy-rejected readings, visualizer mode) the connection can idle out through proxies. Add the same 25 s ping loop; consider server-side WS `ping()` frames as the canonical mechanism for all clients.
- **3.2 The `join` (viewer) protocol message is dead code.** Web visualizer mode still sends `register` and merely suppresses `speed` sends (index.html:2489, 2801); Android does the same (`LocationTrackingService.kt:296–299`). Either adopt `join` everywhere or delete it from the server.
- **3.3 Speed-derivation differs.** Web falls back to haversine distance/Δt when `coords.speed` is null (index.html:2680–2692); Android reports 0.0 (`LocationTrackingService.kt:223–227`). Port the fallback into the shared module and Wear.
- **3.4 Server URL duplication and drift in web.** `waypoints.html:188–189` has its own default (`...koyeb.app/` — trailing slash, unlike index) and its own naive ws→http conversion instead of `getHttpUrl` (double-slash API paths when the stored URL has a trailing slash). Extract a tiny shared JS module (see Phase 3) or at minimum align the defaults and reuse one converter.
- **3.5 Broadcast amplification.** Every `speed` message re-broadcasts the *entire* user list to the whole group (server.js:154): N users at 1 Hz ⇒ N² messages/s. At 10 users that is already 100 msg/s per group. Coalesce: broadcast at a fixed 1 Hz tick per group (or send single-user deltas).
- **3.6 i18n leaks.** Android hardcodes `"Velocidad: %.1f %s"` in the notification (`LocationTrackingService.kt:480`) and English literals in MainActivity toasts (`"Fullscreen map - coming soon"`, `"FIT track saved successfully"`, `"🔔 Group Horn!"`). Move to string resources; add the missing ones to values-es/fr/de.
- **3.7 Repo hygiene.** ~15 crash/debug logs, `screen.png`, and compiled Garmin binaries (`garmin/bin/`, `garmin/compiled/`) are committed at `android/` root. Delete and gitignore (`*.txt` logs, `garmin/bin/`, `garmin/compiled/`).
- **3.8 Garmin → server loop is open.** `Communications.transmit()` fires (GpsTrackerApp.mc:53) but the Android app has no Connect IQ receiver, so Garmin data never reaches the phone or server (documented as future work in `android/GARMIN_INTEGRATION.md`). Until the `.aar` bridge lands, the Garmin app is standalone — the README/user manual should say so explicitly.

---

## 4. Web app quality issues (pre-UI-rework)

- **4.1 "PWA" without a service worker.** There is no offline capability at all; the manifest is a base64 data URI; icons are emoji-in-SVG data URIs. iOS/Android install works but the app is blank offline.
- **4.2 Production CDN dependencies.** `cdn.tailwindcss.com` (explicitly not for production — runtime JIT in the browser, console warning, ~300 kB), Leaflet from unpkg, FIT writer from jsdelivr. Any CDN hiccup or offline start breaks the app.
- **4.3 Accessibility.** `maximum-scale=1.0, user-scalable=no` blocks zoom (WCAG 1.4.4); emoji-as-icons have no labels; color-only status indicators.
- **4.4 Architecture.** One 3,000-line HTML file; the test suite literally regex-extracts function bodies to test them (`netlify/test/frontend.test.js:11`). This caps testability and makes every change risky.
- **4.5 Full `innerHTML` re-render of the user list on every broadcast** (up to N×1 Hz) — DOM churn, focus/selection loss, and it is the XSS surface of 1.1.
- **4.6 `beforeunload` calls an async `stopTracking()`** (index.html:3029) — the `keepalive: true` fetch usually survives, but the WS `close()` races it; acceptable, but worth a `visibilitychange`-based submit as backup.

## 5. Server/deployment misc

- Node 18 base image (`Dockerfile`) — EOL April 2025; bump to `node:22-alpine`.
- `GET /groups` exposes all groups and user positions publicly — fine for a toy, but document it (privacy).
- No graceful shutdown handling (SIGTERM → close WS, drain pool) — Koyeb redeploys drop clients mid-write.
- CLAUDE.md says `node --test test/` — that fails on Node 22 (directory isn't auto-expanded when invoked from repo root); use `cd server && node --test` or name the file.

---

## 6. UI modernization plan (Surfr-inspired)

Surfr's signature look: dark-first, map-as-background, big bold numerals, session cards, leaderboards, minimal chrome. Applied to this app:

### Design system (shared across web + Android)
1. **Dark-first palette** — near-black surfaces (#0B1220-ish), one electric accent (cyan/teal), semantic green/amber/red for status. Light theme derived, not the default.
2. **Typography** — numeric-forward: large tabular-lining figures for speed (`font-variant-numeric: tabular-nums`), Inter/system stack. Speed is the hero: one huge dial value, units small.
3. **Iconography** — replace all emoji (🚀📢🗺️⚙️) with a consistent SVG set (Lucide/Material Symbols), inline-embedded (no CDN).
4. **Layout** — full-bleed map as the primary surface; stats and user list in a draggable bottom sheet; top status pill (GPS/connection); FAB-style horn button. Bottom navigation: **Track · Map · Leaderboard · History · Settings**.

### Web implementation
- **Phase A (restructure):** move `netlify/` to a Vite project — `src/{ws.js,gps.js,stats.js,ui/,i18n.js}` — Tailwind as a build dependency, Leaflet bundled, one shared config module used by both index and waypoints pages. Keep the existing function-extraction tests green during the move by porting them to real ESM imports (they get *simpler*).
- **Phase B (screens):** map-first tracking screen with bottom sheet stats; animated speed dial; jump events as an overlay toast/leaderboard entry rather than a text message; session-summary card on stop (max / 10 s / 500 m / distance / duration + FIT download + share image).
- **Phase C (PWA):** real `manifest.webmanifest` + icons, service worker (precache app shell, offline start, queue history submits with Background Sync), remove `user-scalable=no`.
- **Phase D (leaderboard):** group leaderboard view fed by existing `users` broadcast (live) and `speed_history` (all-time) — the server data is already there; it just needs a screen.

### Android implementation
- The app is split-brain: `MainActivity` is XML/View-based while `JumpTrackerActivity` is already Compose. **Unify on Compose + Material 3** (dynamic color, dark default), one `MaterialTheme` matching the web palette.
- Same screen structure (map-first, bottom sheet, bottom nav). Replace toast-based events with snackbars/overlays; use proper notification icon (currently `android.R.drawable.ic_menu_mylocation`, LocationTrackingService.kt:481).
- Wear: align tiles/typography with the new palette; Garmin: keep native look, adopt naming/labels.

---

## 7. Implementation plan (phased, ordered)

### Phase 0 — Hygiene & safety net (small)
1. Add GitHub Actions CI: server tests, frontend tests, `./gradlew test` (+ `assembleDebug` with a dummy Maps key).
2. Delete committed logs/binaries; extend `.gitignore` (`android/*.txt`, `garmin/bin/`, `garmin/compiled/`).
3. Bump Dockerfile to node:22-alpine; fix CLAUDE.md test command.

### Phase 1 — Security & correctness hotfixes (small–medium; ship independently)
4. `escapeHtml()` at all web interpolation sites (index + waypoints). **(1.1)**
5. XML-escape KML output. **(1.2)**
6. Fix Android 500 m distance bug. **(2.1)** — add a unit test around `handleLocationUpdate` math (extract the stats math into a testable pure class in `shared/`).
7. Remove Android periodic history submission. **(2.2)**
8. Server: server-side `receivedAt` for eviction/KML; lowercase groupName in REST + waypoints; input length/charset caps; per-connection msg rate cap; sweep `hornRateLimit`; reset `hasChanges` per group. **(2.4–2.7, 1.4)**
9. DB TLS: proper CA or env-gated relaxation. **(1.3)**

### Phase 2 — Protocol & behavior standardization (medium)
10. Write `PROTOCOL.md` (single source of truth: message schemas, units, thresholds, keep-alive, group normalization). Point all four clients' docs at it.
11. Android keep-alive ping (25 s), matching web; consider server WS `ping()` frames long-term.
12. Port web's speed fallback (distance/Δt) into `shared/` and Wear.
13. Adopt `join` for viewer mode on web + Android, or remove it server-side — one or the other.
14. Coalesce `users` broadcasts to 1 Hz per group. **(3.5)**
15. Jump algorithm parity: fix Garmin gravity/null-altitude bugs, align Kalman params, document tuned constants in one table. **(2.3)**
16. Android i18n cleanup (notification + toasts → string resources).

### Phase 3 — Web modernization (large)
17. Vite restructure (Phase A above) — no visual change, tests ported to real imports.
18. New UI (Phase B) — design tokens, map-first layout, speed dial, bottom sheet, session summary.
19. Real PWA (Phase C) — manifest, icons, service worker, offline shell, zoom re-enabled.
20. Leaderboard screen (Phase D).

### Phase 4 — Android modernization (large)
21. Compose + Material 3 migration of MainActivity/Settings/History/Waypoints, shared theme with web palette.
22. Map-first layout + bottom nav; fix notification icon; snackbar events.
23. Wear visual alignment.

### Phase 5 — Feature parity & stretch
24. Garmin↔phone bridge via Connect IQ Android SDK `.aar` (closes 3.8) — then Garmin jumps/speeds reach the group live.
25. Jump display on web (live overlay + history), jump leaderboard.
26. Waypoints on Wear (read-only list/map) if desired; FIT export on Wear.

Suggested sequencing: Phase 0+1 together as the first PR series (highest risk-reduction per line changed), Phase 2 next (unblocks everything else being consistent), then 3 and 4 can proceed in parallel, 5 last.
