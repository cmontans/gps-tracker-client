# 3WB Speed Tracker — Web v2.0

A ground-up, Surfr-inspired rebuild of the web client: dark-first, map-forward,
big tabular speed dial, bottom navigation, and a live + all-time group
leaderboard. Built with Vite and split into ES modules (no CDN dependencies —
Tailwind/Leaflet are bundled), so it works fully offline as a PWA.

This is a **separate app** from the original single-file client in `../netlify/`,
which remains the currently deployed version. v2.0 speaks the exact same
WebSocket/REST protocol (`docs/PROTOCOL.md`) and interoperates live with the web,
Android, and Garmin clients.

## Structure
```
web-v2/
├── index.html          # thin shell, loads src/main.js
├── src/
│   ├── main.js         # app orchestration: state, GPS, wiring, map, leaderboard
│   ├── ui.js           # pure view builders (shell, screens, dial, rows, toasts)
│   ├── protocol.js     # WebSocket client (register/speed/horn/jump, keep-alive, reconnect)
│   ├── stats.js        # speed math (SessionSpeedStats, SpeedCalculator, haversine)
│   ├── i18n.js         # es / en / fr / de
│   ├── icons.js        # inline SVG icon set
│   └── styles.css      # Surfr design system (dark-first, light theme included)
├── public/             # manifest, icons, service worker (served at web root)
└── test/               # node --test unit tests for stats.js
```

## Commands
```bash
cd web-v2
npm install
npm run dev        # Vite dev server
npm run build      # -> dist/
npm run preview    # serve the production build
npm test           # node --test (speed-math unit tests)
```

## Deploying to Netlify (separate site)
The original app deploys `netlify/` with no build step; v2.0 needs a build.
Deploy it as its own Netlify site with:
- **Base directory:** `web-v2`
- **Build command:** `npm run build`
- **Publish directory:** `web-v2/dist`

## Notes
- Speed math mirrors the Android shared helpers and `docs/PROTOCOL.md` §3.
- Track export is GPX (self-contained, no external library).
- Server URL, language, units, auto-center, voice, and viewer mode are in Settings.
