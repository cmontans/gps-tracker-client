// WebSocket client for the 3WB protocol (see docs/PROTOCOL.md).
// Handles register/speed/horn/jump, 25s keep-alive ping, and reconnect backoff.

export const DEFAULT_SERVER = 'wss://urban-ricca-cmontans-34613e13.koyeb.app';

export function wsToHttp(wsUrl) {
  const url = (wsUrl || '').trim().replace(/\/+$/, '');
  if (url.startsWith('wss://')) return url.replace('wss://', 'https://');
  if (url.startsWith('ws://')) return url.replace('ws://', 'http://');
  if (url.startsWith('http')) return url;
  if (url.includes('localhost') || url.includes('127.0.0.1')) return `http://${url}`;
  return `https://${url}`;
}

export class TrackerSocket {
  constructor(handlers = {}) {
    this.h = handlers;
    this.ws = null;
    this.keepAlive = null;
    this.reconnectAttempts = 0;
    this.maxReconnects = 10;
    this.manualClose = false;
    this.identity = null; // { userId, userName, groupName, serverUrl }
  }

  connect(identity) {
    this.identity = identity;
    this.manualClose = false;
    try {
      this.ws = new WebSocket(identity.serverUrl);
    } catch (e) {
      this.h.onError?.('No se pudo conectar: ' + e.message);
      return;
    }

    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
      this.send({ type: 'register', userId: identity.userId, userName: identity.userName, groupName: identity.groupName });
      this.startKeepAlive();
      this.h.onConnected?.();
    };
    this.ws.onmessage = (ev) => {
      let data;
      try { data = JSON.parse(ev.data); } catch { return; }
      if (data.type === 'users') this.h.onUsers?.(data.users || []);
      else if (data.type === 'group-horn') this.h.onHorn?.(data);
      else if (data.type === 'group-jump') this.h.onJump?.(data);
      else if (data.type === 'error') this.h.onError?.(data.message);
    };
    this.ws.onerror = () => this.h.onDisconnected?.();
    this.ws.onclose = () => {
      this.stopKeepAlive();
      this.h.onDisconnected?.();
      if (!this.manualClose && this.reconnectAttempts < this.maxReconnects) {
        this.reconnectAttempts++;
        const delay = Math.min(1000 * 2 ** this.reconnectAttempts, 30000);
        setTimeout(() => { if (!this.manualClose) this.connect(this.identity); }, delay);
      }
    };
  }

  send(obj) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) this.ws.send(JSON.stringify(obj));
  }

  sendSpeed(p) {
    this.send({ type: 'speed', userId: this.identity.userId, userName: this.identity.userName, groupName: this.identity.groupName, ...p, timestamp: Date.now() });
  }
  sendHorn() {
    this.send({ type: 'group-horn', userId: this.identity.userId, userName: this.identity.userName, groupName: this.identity.groupName });
  }

  startKeepAlive() {
    this.stopKeepAlive();
    this.keepAlive = setInterval(() => this.send({ type: 'ping' }), 25000);
  }
  stopKeepAlive() { if (this.keepAlive) { clearInterval(this.keepAlive); this.keepAlive = null; } }

  close() { this.manualClose = true; this.stopKeepAlive(); if (this.ws) { this.ws.close(); this.ws = null; } }
  get isOpen() { return this.ws && this.ws.readyState === WebSocket.OPEN; }
}
