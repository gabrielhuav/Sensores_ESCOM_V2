# Sistema de Asientos (Pupitres) - Guía de Uso

## 📋 Descripción

Sistema que permite a los jugadores sentarse en pupitres dentro de los salones del juego. Cada pupitre solo puede ser ocupado por un jugador a la vez, y los asientos se liberan automáticamente cuando el jugador se desconecta.

## 🎯 Características

- ✅ **Ocupación exclusiva**: Solo un jugador puede ocupar un pupitre a la vez
- ✅ **Persistencia de sesión**: Los asientos se mantienen mientras el jugador esté conectado
- ✅ **Liberación automática**: Los asientos se liberan al desconectarse
- ✅ **Sincronización en tiempo real**: Todos los jugadores ven quién está sentado
- ✅ **Multi-mapa**: Sistema funciona en cualquier salón/mapa

## 🔌 API WebSocket

### Sentarse en un Pupitre

**Mensaje del cliente al servidor:**
```json
{
  "type": "sit",
  "id": "player_id_12345",
  "playerName": "Juan Pérez García",
  "map": "escom_salon_2001",
  "x": 4,
  "y": 15
}
```

**Respuesta del servidor al cliente:**
```json
{
  "type": "sit_response",
  "success": true,
  "message": "Te sentaste en el pupitre (4, 15)",
  "deskPosition": { "x": 4, "y": 15 }
}
```

**Broadcast a todos los jugadores (si éxito):**
```json
{
  "type": "player_seated",
  "playerId": "player_id_12345",
  "playerName": "Juan Pérez García",
  "map": "escom_salon_2001",
  "x": 4,
  "y": 15
}
```

### Levantarse de un Pupitre

**Mensaje del cliente al servidor:**
```json
{
  "type": "stand",
  "id": "player_id_12345",
  "map": "escom_salon_2001"
}
```

**Respuesta del servidor al cliente:**
```json
{
  "type": "stand_response",
  "success": true,
  "message": "Te levantaste del pupitre (4, 15)",
  "deskPosition": { "x": 4, "y": 15 }
}
```

**Broadcast a todos los jugadores (si éxito):**
```json
{
  "type": "player_stood",
  "playerId": "player_id_12345",
  "map": "escom_salon_2001",
  "x": 4,
  "y": 15
}
```

### Obtener Asientos Ocupados

**Mensaje del cliente al servidor:**
```json
{
  "type": "get_occupied_seats",
  "map": "escom_salon_2001"
}
```

**Respuesta del servidor:**
```json
{
  "type": "occupied_seats",
  "map": "escom_salon_2001",
  "seats": [
    {
      "x": 4,
      "y": 15,
      "playerId": "player_id_12345",
      "playerName": "Juan Pérez García",
      "timestamp": 1699123456789
    },
    {
      "x": 8,
      "y": 15,
      "playerId": "player_id_67890",
      "playerName": "María López",
      "timestamp": 1699123460000
    }
  ]
}
```

## 🌐 API REST

### GET /seats/:map

Obtener todos los asientos ocupados en un mapa.

**Ejemplo:**
```bash
GET http://localhost:3000/seats/escom_salon_2001
```

**Respuesta:**
```json
{
  "success": true,
  "map": "escom_salon_2001",
  "count": 2,
  "seats": [
    {
      "x": 4,
      "y": 15,
      "playerId": "player_id_12345",
      "playerName": "Juan Pérez García",
      "timestamp": 1699123456789
    }
  ]
}
```

### POST /seats/sit

Sentar a un jugador en un pupitre.

**Body:**
```json
{
  "playerId": "player_id_12345",
  "playerName": "Juan Pérez García",
  "map": "escom_salon_2001",
  "x": 4,
  "y": 15
}
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Te sentaste en el pupitre (4, 15)",
  "deskPosition": { "x": 4, "y": 15 }
}
```

**Respuesta error - pupitre ocupado (409):**
```json
{
  "success": false,
  "message": "Pupitre ocupado por María López"
}
```

### POST /seats/stand

Levantar a un jugador de su pupitre.

**Body:**
```json
{
  "playerId": "player_id_12345",
  "map": "escom_salon_2001"
}
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Te levantaste del pupitre (4, 15)",
  "deskPosition": { "x": 4, "y": 15 }
}
```

**Respuesta error - no sentado (404):**
```json
{
  "success": false,
  "message": "No estás sentado en ningún pupitre"
}
```

### DELETE /seats/:map/:playerId

Forzar la liberación de todos los asientos de un jugador en un mapa.

**Ejemplo:**
```bash
DELETE http://localhost:3000/seats/escom_salon_2001/player_id_12345
```

## 🎮 Integración en Android

### 1. Detectar Pupitres

Según `createSalonMatrix()`, los pupitres están en un grid con estas coordenadas:

```javascript
const numRows = 5;
const numCols = 8;
const rowSpacing = 5;
const colSpacing = 4;
const startY = 15;
const startX = 4;

// Los pupitres están en:
// (4, 15), (4, 20), (4, 25), (4, 30), (4, 35)
// (8, 15), (8, 20), (8, 25), (8, 30), (8, 35)
// (12, 15), (12, 20), etc...
```

### 2. Implementar en Kotlin

```kotlin
// En tu Activity del salón
class SalonActivity : AppCompatActivity() {
    
    private var isSeated = false
    private var currentDeskPosition: Pair<Int, Int>? = null
    
    private fun checkIfOnDesk(position: Pair<Int, Int>): Boolean {
        // Verificar si la posición es un pupitre
        val numRows = 5
        val numCols = 8
        val rowSpacing = 5
        val colSpacing = 4
        val startY = 15
        val startX = 4
        
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val deskY = startY + row * rowSpacing
                val deskX = startX + col * colSpacing
                
                if (position.first == deskX && position.second == deskY) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun onButtonAPressed() {
        val currentPos = gameState.playerPosition
        
        if (!isSeated && checkIfOnDesk(currentPos)) {
            // Intentar sentarse
            sitOnDesk(currentPos)
        } else if (isSeated) {
            // Levantarse
            standFromDesk()
        }
    }
    
    private fun sitOnDesk(position: Pair<Int, Int>) {
        val message = JSONObject().apply {
            put("type", "sit")
            put("id", deviceId)
            put("playerName", playerName)
            put("map", "escom_salon_2001")
            put("x", position.first)
            put("y", position.second)
        }
        
        serverConnectionManager.onlineServerManager.send(message.toString())
    }
    
    private fun standFromDesk() {
        val message = JSONObject().apply {
            put("type", "stand")
            put("id", deviceId)
            put("map", "escom_salon_2001")
        }
        
        serverConnectionManager.onlineServerManager.send(message.toString())
    }
    
    // Manejar respuesta del servidor
    override fun onMessageReceived(message: String) {
        val json = JSONObject(message)
        
        when (json.getString("type")) {
            "sit_response" -> {
                val success = json.getBoolean("success")
                val msg = json.getString("message")
                
                if (success) {
                    isSeated = true
                    val deskPos = json.getJSONObject("deskPosition")
                    currentDeskPosition = Pair(deskPos.getInt("x"), deskPos.getInt("y"))
                }
                
                showToast(msg)
            }
            
            "stand_response" -> {
                val success = json.getBoolean("success")
                val msg = json.getString("message")
                
                if (success) {
                    isSeated = false
                    currentDeskPosition = null
                }
                
                showToast(msg)
            }
            
            "player_seated" -> {
                // Otro jugador se sentó
                val playerId = json.getString("playerId")
                val x = json.getInt("x")
                val y = json.getInt("y")
                
                // Marcar el pupitre como ocupado visualmente
                markDeskOccupied(x, y, playerId)
            }
            
            "player_stood" -> {
                // Otro jugador se levantó
                val x = json.getInt("x")
                val y = json.getInt("y")
                
                // Marcar el pupitre como libre
                markDeskFree(x, y)
            }
        }
    }
}
```

### 3. Mostrar UI al Jugador

```kotlin
private fun updatePlayerPosition(position: Pair<Int, Int>) {
    runOnUiThread {
        gameState.playerPosition = position
        
        // Verificar si está sobre un pupitre
        if (checkIfOnDesk(position) && !isSeated) {
            showToast("Presiona A para sentarte")
        }
    }
}
```

## 🧪 Pruebas con PowerShell

### Sentar un jugador
```powershell
$body = @{
    playerId = "test_player_1"
    playerName = "Juan Pérez"
    map = "escom_salon_2001"
    x = 4
    y = 15
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:3000/seats/sit" -Method POST -Body $body -ContentType "application/json"
```

### Ver asientos ocupados
```powershell
Invoke-RestMethod -Uri "http://localhost:3000/seats/escom_salon_2001"
```

### Levantar un jugador
```powershell
$body = @{
    playerId = "test_player_1"
    map = "escom_salon_2001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:3000/seats/stand" -Method POST -Body $body -ContentType "application/json"
```

## 📝 Notas Importantes

1. **Coordenadas de pupitres**: Los pupitres están definidos en `collisionMatrices.js` en la función `createSalonMatrix()`
2. **Un jugador, un asiento**: Un jugador solo puede estar sentado en un pupitre a la vez
3. **Liberación automática**: Al desconectarse, todos los asientos del jugador se liberan
4. **Sincronización**: Todos los clientes reciben notificaciones cuando alguien se sienta o levanta
5. **Persistencia**: Los asientos solo persisten durante la sesión, no se guardan en base de datos

## 🔍 Logs del Servidor

El servidor muestra logs informativos:

```
✅ Juan Pérez se sentó en pupitre (4, 15) del mapa escom_salon_2001
🚶 Juan Pérez se levantó del pupitre (4, 15) del mapa escom_salon_2001
🔓 Liberado pupitre (4, 15) del mapa escom_salon_2001 por desconexión de Juan Pérez
```

## 🚀 Próximas Mejoras

- [ ] Persistir asientos en base de datos para reconexiones
- [ ] Tiempo máximo de ocupación (AFK detection)
- [ ] Reserva de asientos
- [ ] Visualización de nombre del ocupante en el mapa
- [ ] Sistema de asientos asignados por profesor
