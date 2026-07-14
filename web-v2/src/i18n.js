// Compact i18n for v2.0. Covers the strings surfaced in the new UI.
const STRINGS = {
  es: {
    track: 'Rastrear', map: 'Mapa', board: 'Ranking', history: 'Historial', settings: 'Ajustes',
    yourName: 'Tu nombre', namePh: 'Ej: Juan, María…', groupName: 'Grupo', groupPh: 'Ej: Amigos, Carrera2024…',
    nameHelper: 'Así te verán los demás', groupHelper: 'Solo verás usuarios de tu mismo grupo',
    start: 'Iniciar seguimiento', stop: 'Detener', currentSpeed: 'Velocidad actual',
    avg: 'Promedio', maxSession: 'Máx. sesión', avg10s: 'Prom. 10s', max10s: 'Máx. 10s', avg500m: 'Prom. 500m', max500m: 'Máx. 500m',
    reset: 'Reset', horn: 'Bocina', download: 'Descargar FIT',
    connected: 'Conectado', disconnected: 'Sin conexión', gpsActive: 'GPS activo', gpsSearching: 'Buscando GPS',
    users: 'Riders en vivo', noUsers: 'Nadie conectado todavía', you: 'TÚ',
    liveBoard: 'Ranking en vivo', allTime: 'Récords (todos)', noRecords: 'Sin registros aún',
    serverUrl: 'Servidor WebSocket', language: 'Idioma', autoCenter: 'Auto-centrar el mapa', voice: 'Anunciar velocidad por voz',
    visualizer: 'Modo visualizador (solo ver)', speedUnit: 'Unidad de velocidad', minSpeed: 'Velocidad mín. para anunciar',
    enterName: 'Introduce tu nombre', hornReceived: '📢 {name} activó la bocina', jumpAlert: '🦘 {name} saltó {h}m ({t}s)',
    visualizerActive: 'Modo visualizador activo — no publicas tu posición',
    accuracy: 'Precisión', coords: 'Coordenadas', yourPos: 'Tú', others: 'Otros', track_: 'Trayectoria',
    sessionDone: 'Sesión completada', topSpeed: 'Velocidad máxima', distance: 'Distancia', duration: 'Duración', close: 'Cerrar'
  },
  en: {
    track: 'Track', map: 'Map', board: 'Ranking', history: 'History', settings: 'Settings',
    yourName: 'Your name', namePh: 'e.g. John, Mary…', groupName: 'Group', groupPh: 'e.g. Friends, Race2024…',
    nameHelper: 'How others will see you', groupHelper: 'You only see users in your group',
    start: 'Start tracking', stop: 'Stop', currentSpeed: 'Current speed',
    avg: 'Average', maxSession: 'Session max', avg10s: 'Avg 10s', max10s: 'Max 10s', avg500m: 'Avg 500m', max500m: 'Max 500m',
    reset: 'Reset', horn: 'Horn', download: 'Download FIT',
    connected: 'Connected', disconnected: 'Offline', gpsActive: 'GPS active', gpsSearching: 'Searching GPS',
    users: 'Live riders', noUsers: 'No one connected yet', you: 'YOU',
    liveBoard: 'Live ranking', allTime: 'All-time records', noRecords: 'No records yet',
    serverUrl: 'WebSocket server', language: 'Language', autoCenter: 'Auto-center the map', voice: 'Announce speed by voice',
    visualizer: 'Viewer mode (watch only)', speedUnit: 'Speed unit', minSpeed: 'Min speed to announce',
    enterName: 'Enter your name', hornReceived: '📢 {name} sounded the horn', jumpAlert: '🦘 {name} jumped {h}m ({t}s)',
    visualizerActive: 'Viewer mode on — your position is not published',
    accuracy: 'Accuracy', coords: 'Coordinates', yourPos: 'You', others: 'Others', track_: 'Track',
    sessionDone: 'Session complete', topSpeed: 'Top speed', distance: 'Distance', duration: 'Duration', close: 'Close'
  },
  fr: {
    track: 'Suivi', map: 'Carte', board: 'Classement', history: 'Historique', settings: 'Réglages',
    yourName: 'Ton nom', namePh: 'ex : Jean, Marie…', groupName: 'Groupe', groupPh: 'ex : Amis, Course2024…',
    nameHelper: 'Comment les autres te voient', groupHelper: 'Tu ne vois que ton groupe',
    start: 'Démarrer le suivi', stop: 'Arrêter', currentSpeed: 'Vitesse actuelle',
    avg: 'Moyenne', maxSession: 'Max session', avg10s: 'Moy 10s', max10s: 'Max 10s', avg500m: 'Moy 500m', max500m: 'Max 500m',
    reset: 'Reset', horn: 'Klaxon', download: 'Télécharger FIT',
    connected: 'Connecté', disconnected: 'Hors ligne', gpsActive: 'GPS actif', gpsSearching: 'Recherche GPS',
    users: 'Riders en direct', noUsers: 'Personne connecté', you: 'TOI',
    liveBoard: 'Classement en direct', allTime: 'Records', noRecords: 'Aucun record',
    serverUrl: 'Serveur WebSocket', language: 'Langue', autoCenter: 'Centrer la carte', voice: 'Annoncer la vitesse',
    visualizer: 'Mode spectateur', speedUnit: 'Unité de vitesse', minSpeed: 'Vitesse min. à annoncer',
    enterName: 'Entre ton nom', hornReceived: '📢 {name} a klaxonné', jumpAlert: '🦘 {name} a sauté {h}m ({t}s)',
    visualizerActive: 'Mode spectateur — position non publiée',
    accuracy: 'Précision', coords: 'Coordonnées', yourPos: 'Toi', others: 'Autres', track_: 'Trajectoire',
    sessionDone: 'Session terminée', topSpeed: 'Vitesse max', distance: 'Distance', duration: 'Durée', close: 'Fermer'
  },
  de: {
    track: 'Tracking', map: 'Karte', board: 'Rangliste', history: 'Verlauf', settings: 'Einstellungen',
    yourName: 'Dein Name', namePh: 'z.B. Jan, Maria…', groupName: 'Gruppe', groupPh: 'z.B. Freunde, Rennen2024…',
    nameHelper: 'So sehen dich andere', groupHelper: 'Du siehst nur deine Gruppe',
    start: 'Tracking starten', stop: 'Stopp', currentSpeed: 'Aktuelle Geschw.',
    avg: 'Schnitt', maxSession: 'Max Session', avg10s: 'Ø 10s', max10s: 'Max 10s', avg500m: 'Ø 500m', max500m: 'Max 500m',
    reset: 'Reset', horn: 'Hupe', download: 'FIT laden',
    connected: 'Verbunden', disconnected: 'Offline', gpsActive: 'GPS aktiv', gpsSearching: 'GPS-Suche',
    users: 'Live-Rider', noUsers: 'Noch niemand verbunden', you: 'DU',
    liveBoard: 'Live-Rangliste', allTime: 'Rekorde', noRecords: 'Noch keine Rekorde',
    serverUrl: 'WebSocket-Server', language: 'Sprache', autoCenter: 'Karte zentrieren', voice: 'Geschwindigkeit ansagen',
    visualizer: 'Zuschauermodus', speedUnit: 'Geschwindigkeitseinheit', minSpeed: 'Min. Geschw. zum Ansagen',
    enterName: 'Gib deinen Namen ein', hornReceived: '📢 {name} hat gehupt', jumpAlert: '🦘 {name} sprang {h}m ({t}s)',
    visualizerActive: 'Zuschauermodus — Position wird nicht geteilt',
    accuracy: 'Genauigkeit', coords: 'Koordinaten', yourPos: 'Du', others: 'Andere', track_: 'Route',
    sessionDone: 'Session beendet', topSpeed: 'Höchstgeschw.', distance: 'Distanz', duration: 'Dauer', close: 'Schließen'
  }
};

let lang = localStorage.getItem('lang') || 'es';
export function setLang(l) { lang = STRINGS[l] ? l : 'es'; localStorage.setItem('lang', lang); }
export function getLang() { return lang; }
export function t(key, vars) {
  let s = (STRINGS[lang] && STRINGS[lang][key]) || STRINGS.en[key] || key;
  if (vars) for (const k in vars) s = s.replace(`{${k}}`, vars[k]);
  return s;
}
