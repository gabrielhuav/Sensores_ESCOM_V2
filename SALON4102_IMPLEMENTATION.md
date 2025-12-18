# Salón 4102 - Guía de Implementación

## 📋 Descripción General

Se ha creado un nuevo salón (Salón 4102) en el Edificio IA con las siguientes características:
- **Ubicación**: Accesible desde BuildingEdificioIA_Medio en coordenadas (11, 5)
- **Funcionalidad**: Sistema integrado de asientos + registro automático de asistencia
- **Imagen del mapa**: `escom_salon4102.png`
- **Paquete**: `buildingIA.salon`

## 🎯 Funcionalidades Implementadas

### 1. Sistema de Asientos
- **40 pupitres** distribuidos en 5 filas × 8 columnas
- Posiciones de pupitres: desde (4,15) hasta (32,35)
  - Espaciado horizontal: 4 unidades
  - Espaciado vertical: 5 unidades
- Los estudiantes pueden **sentarse** presionando el botón A cuando están sobre un pupitre
- Solo **un estudiante por pupitre** (exclusividad garantizada por el servidor)
- Los asientos se **liberan automáticamente** cuando el jugador sale del salón o se desconecta

### 2. Registro de Asistencia Automático
- **Cuando un estudiante se sienta**, automáticamente se registra su asistencia
- Información registrada:
  - `phoneID`: Android Device ID
  - `fullName`: Nombre del jugador
  - `group`: "7CV2"
  - `attendanceTime`: Timestamp con zona horaria de México (UTC-6)
- **Una asistencia por día**: El sistema valida que no se dupliquen registros

### 3. Integración con WebSocket
Mensajes WebSocket implementados:
- **`sit`**: Solicitud para sentarse en un pupitre
  ```json
  {
    "type": "sit",
    "id": "device_id",
    "playerName": "nombre",
    "map": "escom_salon4102",
    "x": 4,
    "y": 15
  }
  ```
- **`sit_response`**: Respuesta del servidor
  ```json
  {
    "type": "sit_response",
    "success": true,
    "message": "Te has sentado en el pupitre",
    "deskPosition": {"x": 4, "y": 15}
  }
  ```
- **`stand`**: Levantarse del pupitre
- **`get_occupied_seats`**: Obtener lista de pupitres ocupados
- **`player_seated`**: Broadcast a todos cuando alguien se sienta
- **`player_stood`**: Broadcast a todos cuando alguien se levanta

## 📁 Archivos Creados/Modificados

### Archivos Nuevos
1. **Salon4102.kt**
   - Ruta: `app/src/main/java/.../buildingIA/salon/Salon4102.kt`
   - Activity principal del salón
   - 650+ líneas de código
   - Funcionalidades:
     - Detección de pupitres con `checkIfOnDesk()`
     - Sentarse/levantarse con `sitOnDesk()` y `standFromDesk()`
     - Registro automático de asistencia en `registerAttendanceOnSit()`
     - Manejo de mensajes WebSocket
     - Sincronización con otros jugadores

2. **activity_salon4102.xml**
   - Ruta: `app/src/main/res/layout/activity_salon4102.xml`
   - Layout con:
     - MapView container
     - Controles de movimiento (N, S, E, W)
     - Botón A (naranja) para sentarse/levantarse
     - TextView de estado

### Archivos Modificados
1. **MapMatrixProvider.kt**
   - Agregada constante: `MAP_SALON4102 = "escom_salon4102"`
   - Normalización del nombre del mapa en `normalizeMapName()`
   - Posición inicial: `Pair(20, 35)` en `getInitialPositionForMap()`

2. **BuildingEdificioIA_Medio.kt**
   - Modificada función `checkPositionForMapChange()`:
     - Cambio en posición (11,5): de registro de asistencia a entrada del salón
     - Mensaje: "Presiona A para entrar al Salón 4102"
   - Actualizado `buttonA.setOnClickListener()` con caso "salon4102"
   - Nueva función `startSalon4102Activity()`:
     - Crea Intent con posición inicial (20,35)
     - Pasa PLAYER_NAME, IS_SERVER, IS_CONNECTED, PREVIOUS_POSITION

3. **AndroidManifest.xml**
   - Registrada actividad:
   ```xml
   <activity
       android:name=".presentation.locations.buildings.buildingIA.salon.Salon4102"
       android:configChanges="orientation|screenSize|keyboardHidden"/>
   ```

## 🚀 Flujo de Uso

### Para el Estudiante
1. **Navegar** al Edificio IA planta media
2. **Moverse** a las coordenadas (11, 5)
3. Ver mensaje: "Presiona A para entrar al Salón 4102"
4. **Presionar A** para entrar al salón
5. **Caminar** sobre cualquier pupitre libre
6. Ver mensaje: "Presiona A para sentarte"
7. **Presionar A** para sentarse
8. El sistema automáticamente:
   - Ocupa el pupitre exclusivamente
   - Registra tu asistencia del día
   - Notifica a otros jugadores
9. Para **levantarse**, presionar A nuevamente
10. Para **salir del salón**, presionar botón "Salir" (vuelve a posición 11,5)

### Posiciones de Pupitres (Referencia)
```
Fila 1 (Y=15): X = 4, 8, 12, 16, 20, 24, 28, 32
Fila 2 (Y=20): X = 4, 8, 12, 16, 20, 24, 28, 32
Fila 3 (Y=25): X = 4, 8, 12, 16, 20, 24, 28, 32
Fila 4 (Y=30): X = 4, 8, 12, 16, 20, 24, 28, 32
Fila 5 (Y=35): X = 4, 8, 12, 16, 20, 24, 28, 32
```

## 🔧 Integración con el Servidor

El sistema ya está completamente integrado con el servidor Node.js existente que tiene:
- ✅ Función `sitPlayer(playerId, playerName, mapName, x, y)`
- ✅ Función `standPlayer(playerId, mapName)`
- ✅ Objeto `seatedPlayers` para tracking
- ✅ Auto-liberación en `ws.on("close")`
- ✅ Endpoints REST:
  - GET `/seats/:map` - Ver asientos ocupados
  - POST `/seats/sit` - Sentarse
  - POST `/seats/stand` - Levantarse
  - DELETE `/seats/:map/:playerId` - Liberar asiento

## 📝 Validaciones del Sistema

### Validaciones de Asientos (Servidor)
- ✅ No se puede sentar si el pupitre está ocupado
- ✅ No se puede sentar si ya estás sentado en otro lugar
- ✅ No se puede levantar si no estás sentado
- ✅ Los asientos se liberan automáticamente al desconectarse

### Validaciones de Asistencia (Servidor)
- ✅ Solo una asistencia por estudiante por día
- ✅ Registro con timestamp en zona horaria de México
- ✅ Campos requeridos: phoneID, fullName, group
- ✅ Almacenamiento en base de datos con Prisma

## 🎨 Diseño Visual
- Botón A en color **naranja** (`holo_orange_dark`) para destacar la acción de sentarse
- Controles de movimiento en **grid 3×3** (esquina inferior izquierda)
- TextView de estado muestra: "Salón 4102" o "Asistencia registrada - Salón 4102"
- Toast notifications para feedback inmediato

## 🔄 Estados del Jugador en el Salón

```kotlin
// Variables de control
var isSeated = false  // ¿Está sentado?
var currentDeskPosition: Pair<Int, Int>? = null  // Pupitre actual
var attendanceRegistered = false  // ¿Ya registró asistencia?
```

## 🧪 Pruebas Recomendadas

### Prueba 1: Sentarse y Registrar Asistencia
1. Entrar al salón
2. Ir a pupitre (4, 15)
3. Presionar A
4. Verificar toast: "Te has sentado..."
5. Verificar toast: "📝 Registrando tu asistencia..."
6. Verificar toast: "✅ Asistencia registrada..."

### Prueba 2: Conflicto de Pupitres
1. Jugador A se sienta en (8, 15)
2. Jugador B intenta sentarse en (8, 15)
3. Verificar rechazo: "Este pupitre ya está ocupado"

### Prueba 3: Liberación Automática
1. Sentarse en cualquier pupitre
2. Salir del salón con botón "Salir"
3. Verificar que el pupitre quede libre para otros

### Prueba 4: Asistencia Duplicada
1. Sentarse y registrar asistencia
2. Levantarse
3. Sentarse en otro pupitre
4. Verificar que no se registre asistencia nuevamente

## 📊 Logs de Depuración

El sistema genera logs con TAG "Salon4102":
```
D/Salon4102: Intentando sentarse en pupitre (4, 15)
D/Salon4102: Respuesta sit: Te has sentado en el pupitre
D/Salon4102: Registrando asistencia al sentarse - ID: xxx, Nombre: xxx, Grupo: 7CV2
D/Salon4102: Asistencia registrada exitosamente
D/Salon4102: Jugador player1 se sentó en (8, 15)
```

## 🆘 Troubleshooting

### Problema: No puedo sentarme
- ✅ Verificar que estás en coordenadas exactas de un pupitre
- ✅ Verificar conexión al servidor WebSocket
- ✅ Revisar logs del servidor para mensajes "sit"

### Problema: No se registra asistencia
- ✅ Verificar que te sentaste exitosamente primero
- ✅ Revisar que el servidor tenga acceso a la base de datos
- ✅ Verificar endpoint POST `/attendance/register`

### Problema: El asiento no se libera
- ✅ Verificar que el servidor recibe mensaje "stand"
- ✅ Revisar logs de `ws.on("close")` en el servidor
- ✅ Usar endpoint DELETE manual si es necesario

## 🔐 Seguridad

- El Android Device ID se usa como identificador único (no modificable por usuario)
- Las validaciones de duplicados se hacen en el servidor
- Los timestamps usan zona horaria del servidor (UTC-6)
- Las posiciones de pupitres son validadas contra la matriz del servidor

## 📖 Referencias Adicionales

- [SEATS_SYSTEM_GUIDE.md](./SEATS_SYSTEM_GUIDE.md) - Guía completa del sistema de asientos
- [test-seats.js](./Online-Server/test-seats.js) - Suite de pruebas automatizadas
- [server.js](./Online-Server/server.js) - Implementación del servidor
- [collisionMatrices.js](./Online-Server/collisionMatrices.js) - Definición de posiciones de pupitres

---

**Fecha de creación**: 2024
**Versión**: 1.0
**Autor**: Sistema de Sensores ESCOM V2
