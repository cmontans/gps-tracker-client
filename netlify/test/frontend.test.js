const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');

// Read the index.html content
const htmlPath = path.join(__dirname, '../index.html');
const htmlContent = fs.readFileSync(htmlPath, 'utf8');

// Helper to extract functions from the HTML script tag
function extractFunction(name) {
  const funcDecl = `function ${name}`;
  const index = htmlContent.indexOf(funcDecl);
  if (index === -1) {
    throw new Error(`Could not find function: ${name}`);
  }
  
  // Find the start of the body '{'
  const braceStart = htmlContent.indexOf('{', index);
  if (braceStart === -1) {
    throw new Error(`Could not find opening brace for ${name}`);
  }
  
  // Extract parameters
  const header = htmlContent.slice(index + funcDecl.length, braceStart);
  const params = header.replace(/[()]/g, '').split(',').map(p => p.trim()).filter(Boolean);
  
  // Track braces to find the matching closing brace
  let depth = 1;
  let pos = braceStart + 1;
  while (depth > 0 && pos < htmlContent.length) {
    if (htmlContent[pos] === '{') depth++;
    else if (htmlContent[pos] === '}') depth--;
    pos++;
  }
  
  const body = htmlContent.slice(braceStart + 1, pos - 1);
  return new Function(...params, body);
}

// Extract functions
const getDistance = extractFunction('getDistance');
const getHttpUrl = extractFunction('getHttpUrl');

test('getDistance - calculates distance between two coordinates correctly', () => {
  // Test case: Paris to London
  // Paris: 48.8566 N, 2.3522 E
  // London: 51.5074 N, 0.1278 W (-0.1278)
  const dist = getDistance(48.8566, 2.3522, 51.5074, -0.1278);
  
  // Approximate distance is around 344 km (344000 meters)
  assert.ok(dist > 340000 && dist < 350000, `Distance was ${dist} meters, expected ~344000`);
  
  // Test case: same point should be 0 distance
  assert.strictEqual(getDistance(0, 0, 0, 0), 0);
});

test('getHttpUrl - converts websocket URL to HTTP correctly', () => {
  // Secure Websocket
  assert.strictEqual(
    getHttpUrl('wss://urban-ricca-cmontans-34613e13.koyeb.app'),
    'https://urban-ricca-cmontans-34613e13.koyeb.app'
  );

  // Insecure Websocket
  assert.strictEqual(
    getHttpUrl('ws://localhost:3001'),
    'http://localhost:3001'
  );

  // Already HTTPS
  assert.strictEqual(
    getHttpUrl('https://example.com/'),
    'https://example.com'
  );

  // Already HTTP with trailing slashes
  assert.strictEqual(
    getHttpUrl('http://example.com///'),
    'http://example.com'
  );

  // No protocol - localhost
  assert.strictEqual(
    getHttpUrl('localhost:3000'),
    'http://localhost:3000'
  );

  // No protocol - production domain
  assert.strictEqual(
    getHttpUrl('gps-tracker.koyeb.app'),
    'https://gps-tracker.koyeb.app'
  );

  // Empty or null
  assert.strictEqual(getHttpUrl(''), '');
  assert.strictEqual(getHttpUrl(null), '');
});

test('GPS Speed Fallback calculation simulation', () => {
  // Simulate the speed calculation logic from watchPosition
  const GPS_CONFIG = { MS_TO_KMH: 3.6 };
  
  const lastPosition = {
    coords: { latitude: 48.8566, longitude: 2.3522 },
    timestamp: 1000000
  };
  
  const position = {
    coords: { latitude: 48.8576, longitude: 2.3522 }, // moved N (approx 111m)
    timestamp: 1010000 // 10s elapsed
  };
  
  const distMeters = getDistance(
    lastPosition.coords.latitude,
    lastPosition.coords.longitude,
    position.coords.latitude,
    position.coords.longitude
  );
  
  const timeSeconds = (position.timestamp - lastPosition.timestamp) / 1000;
  assert.strictEqual(timeSeconds, 10);
  
  const speedMs = distMeters / timeSeconds;
  const rawSpeed = speedMs * GPS_CONFIG.MS_TO_KMH;
  
  // 111 meters in 10s is about 11.1 m/s = 40 km/h
  assert.ok(rawSpeed > 38 && rawSpeed < 42, `Calculated fallback speed was ${rawSpeed} km/h`);
});
