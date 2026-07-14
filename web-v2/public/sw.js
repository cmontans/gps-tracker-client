/* Service worker for 3WB Speed Tracker.
 * - Precaches the app shell so the app starts offline.
 * - Stale-while-revalidate for same-origin static + CDN assets (Tailwind/Leaflet).
 * - Never caches API traffic or non-GET requests.
 */
const CACHE = '3wb-tracker-v1';
const SHELL = [
  './',
  './index.html',
  './waypoints.html',
  './manifest.webmanifest',
  './icon.svg',
  './icon-maskable.svg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(SHELL))
      .then(() => self.skipWaiting())
      .catch(() => { /* first-load offline: shell will fill lazily */ })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return; // never cache POST/PUT/DELETE (API writes)

  const url = new URL(req.url);

  // Never cache API or KML endpoints — always go to the network.
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/kml/')) {
    return; // default browser handling (network)
  }

  // Stale-while-revalidate for everything else (app shell, CDN libs, tiles).
  event.respondWith(
    caches.open(CACHE).then((cache) =>
      cache.match(req).then((cached) => {
        const network = fetch(req)
          .then((res) => {
            // Only cache successful, cacheable responses.
            if (res && (res.ok || res.type === 'opaque')) {
              cache.put(req, res.clone()).catch(() => {});
            }
            return res;
          })
          .catch(() => cached);
        return cached || network;
      })
    )
  );
});
