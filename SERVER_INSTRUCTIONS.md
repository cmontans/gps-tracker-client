# Instrucciones para el Servidor - Bocina Grupal

## Resumen
Se ha añadido una funcionalidad de "Bocina Grupal" en el cliente que permite a cualquier usuario activar una bocina potente que suena en todos los usuarios conectados del mismo grupo.

## Cambios necesarios en el servidor

Para que esta funcionalidad funcione completamente, el servidor WebSocket debe manejar el nuevo tipo de mensaje `group-horn`.

### Tipo de mensaje enviado por el cliente:

```json
{
  "type": "group-horn",
  "userId": "user_123...",
  "userName": "Nombre del Usuario",
  "groupName": "nombre_del_grupo",
  "timestamp": 1234567890
}
```

### Implementación requerida en el servidor:

El servidor debe:

1. **Recibir** el mensaje de tipo `group-horn`
2. **Identificar** el grupo del usuario que envió la señal
3. **Distribuir** el mensaje a **todos los usuarios del mismo grupo**
4. **Excluir** opcionalmente al usuario que envió la señal (el cliente ya reproduce el sonido localmente)

### Ejemplo de implementación (Node.js con ws):

```javascript
// En el manejador de mensajes WebSocket
ws.on('message', (message) => {
  const data = JSON.parse(message);

  if (data.type === 'group-horn') {
    console.log(`📢 Bocina activada por ${data.userName} en grupo ${data.groupName}`);

    // Distribuir a todos los usuarios del mismo grupo
    clients.forEach(client => {
      if (client.readyState === WebSocket.OPEN &&
          client.groupName === data.groupName) {
        client.send(JSON.stringify({
          type: 'group-horn',
          userId: data.userId,
          userName: data.userName,
          groupName: data.groupName,
          timestamp: data.timestamp
        }));
      }
    });
  }

  // ... resto de manejadores
});
```

## Comportamiento del cliente

- **Al enviar**: El usuario que pulsa el botón escucha la bocina inmediatamente y envía la señal al servidor
- **Al recibir**: Los demás usuarios del grupo reciben la señal y reproducen el sonido automáticamente con una notificación que indica quién activó la bocina

## Características del sonido

- Sonido generado con Web Audio API
- Duración: 1.5 segundos
- Frecuencias: 220Hz y 330Hz (simulando bocina de auto)
- Volumen: Alto (0.8) con fade out gradual
- Sin archivos de audio externos necesarios

## Seguridad y validación

Se recomienda que el servidor implemente:

1. **Rate limiting**: Limitar la frecuencia con la que un usuario puede activar la bocina (ej: máximo 1 vez cada 5 segundos)
2. **Validación de grupo**: Asegurar que el usuario pertenece al grupo antes de distribuir
3. **Logging**: Registrar quién activa la bocina para auditoría

## Pruebas

Para probar la funcionalidad:

1. Abrir la aplicación en dos dispositivos/pestañas diferentes
2. Configurar el mismo nombre de grupo en ambos
3. Iniciar seguimiento en ambos
4. Pulsar el botón "📢 Bocina Grupal" en uno de ellos
5. Verificar que el sonido se reproduce en ambos dispositivos
