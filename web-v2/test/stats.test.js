import test from 'node:test';
import assert from 'node:assert';
import { SessionSpeedStats, deriveSpeedKmh, getDistance, convertSpeed, escapeHtml } from '../src/stats.js';

test('deriveSpeedKmh uses hardware speed, else distance/time fallback', () => {
  assert.strictEqual(deriveSpeedKmh(true, 10, 0, 0), 36); // 10 m/s -> 36 km/h
  assert.strictEqual(deriveSpeedKmh(false, 0, 100, 10000), 36); // 100m / 10s
  assert.strictEqual(deriveSpeedKmh(false, 0, 0, 1000), 0);
});

test('getDistance approximates Paris->London', () => {
  const d = getDistance(48.8566, 2.3522, 51.5074, -0.1278);
  assert.ok(d > 340000 && d < 350000, `got ${d}`);
  assert.strictEqual(getDistance(0, 0, 0, 0), 0);
});

test('SessionSpeedStats computes 500m average once window has data', () => {
  const s = new SessionSpeedStats();
  let snap;
  for (let i = 0; i <= 7; i++) snap = s.update(36, 36, i * 1000, i === 0 ? 0 : 10);
  assert.ok(snap.avg500m > 0);
  assert.ok(Math.abs(snap.avg500m - 36) < 0.5);
});

test('SessionSpeedStats max uses raw speed, averages use thresholded', () => {
  const s = new SessionSpeedStats();
  const snap = s.update(5, 0, 0, 0); // stationary noise: raw 5, current 0
  assert.strictEqual(snap.max, 5);
  assert.strictEqual(snap.avg, 0);
});

test('convertSpeed converts units', () => {
  assert.strictEqual(convertSpeed(100, 'kmh'), 100);
  assert.ok(Math.abs(convertSpeed(100, 'mph') - 62.1371) < 0.01);
  assert.ok(Math.abs(convertSpeed(100, 'knots') - 53.9957) < 0.01);
});

test('escapeHtml neutralizes markup', () => {
  assert.strictEqual(escapeHtml('<b>&"\''), '&lt;b&gt;&amp;&quot;&#39;');
});
