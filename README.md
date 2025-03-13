# Sensores ESCOM V2 - Navegación y Sincronización Multijugador

## Descripción detallada del proyecto

Este proyecto implementa una aplicación Android que permite a los usuarios navegar por diferentes mapas interactivos de la ESCOM (Escuela Superior de Cómputo) utilizando controles táctiles y sensores del dispositivo. La aplicación cuenta con las siguientes características principales:

- **Navegación entre múltiples mapas**: El usuario puede explorar diferentes áreas del campus, incluyendo:
  - Edificio principal
  - Edificio 2
  - Edificio nuevo
  - Salida del Metro
  - Cafetería
  - Salones de clase (2009, 2010)

- **Sincronización multijugador**: Permite a varios usuarios conectarse mediante:
  - Bluetooth para conexiones locales
  - Servidor WebSocket para conexiones a través de internet

- **Interacción con puntos de interés**: En cada mapa existen puntos específicos donde el usuario puede obtener información adicional presionando el botón A cuando se encuentra en las coordenadas correctas.

- **Sistema de colisiones**: Implementa matrices de colisión para cada mapa, evitando que los jugadores atraviesen obstáculos.

- **Persistencia de estado**: Mantiene el estado del juego (posición, conexiones) durante las transiciones entre mapas.

- **Elementos dinámicos**: En la cafetería, un zombie persigue a los jugadores, añadiendo un elemento de desafío.

La arquitectura del proyecto sigue un patrón modular, donde cada mapa está representado por una actividad independiente que comparte componentes comunes como el sistema de gestión de movimiento, la matriz de colisiones y la comunicación con el servidor.

## Instrucciones paso a paso para ejecutar y probar la aplicación

### Requisitos previos

1. **Software necesario**:
   - Android Studio (versión Arctic Fox o superior)
   - Node.js y npm (para el servidor online)
   - Dispositivo Android con API 24 (Android 7.0) o superior

2. **Hardware recomendado**:
   - Dispositivo Android con soporte para Bluetooth
   - Permisos de ubicación y Bluetooth habilitados

### Configuración del proyecto

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/tu-usuario/Sensores_ESCOM_V2.git
   cd Sensores_ESCOM_V2
   ```

2. **Configurar el servidor online** (opcional para funcionalidad multijugador):
   ```bash
   cd Online-Server
   npm install
   node server.js
   ```
   El servidor se ejecutará en el puerto 8080 por defecto.

3. **Configurar la dirección IP del servidor**:
   - Abrir el archivo `OnlineServerManager.kt`
   - Modificar la constante `SERVER_URL` con la dirección IP de tu servidor:
     ```kotlin
     private const val SERVER_URL = "ws://tu-ip-aqui:8080"
     ```

4. **Compilar y ejecutar la aplicación**:
   - Abrir el proyecto en Android Studio
   - Conectar un dispositivo Android físico o configurar un emulador
   - Ejecutar la aplicación desde Android Studio (Shift+F10)

### Prueba de la aplicación

1. **Navegación básica**:
   - Al iniciar la aplicación, introduce un nombre de usuario
   - Utiliza los botones direccionales (N, S, E, W) para mover al personaje
   - Explora el mapa principal (edificio de ESCOM)

2. **Cambio entre mapas**:
   - Localiza los puntos de transición en cada mapa (generalmente en los bordes o entradas)
   - Al acercarte a estos puntos, el juego te permitirá cambiar de mapa automáticamente
   - Prueba la transición entre el edificio principal y la salida del metro

3. **Interacción con puntos de interés**:
   - En el mapa "Salida Metro", muévete a las coordenadas (35,5), (31,27) o (17,22)
   - Cuando aparezca un mensaje "Presiona A para ver datos...", pulsa el botón A
   - Se mostrará un diálogo con información sobre ese punto específico

4. **Prueba de multijugador local (Bluetooth)**:
   - En un dispositivo, selecciona la opción "Servidor Bluetooth"
   - En otro dispositivo, selecciona "Cliente Bluetooth" y conecta con el primer dispositivo
   - Verifica que puedes ver la posición del otro jugador en tiempo real

5. **Prueba de multijugador online**:
   - Asegúrate de que el servidor Node.js está en ejecución
   - En la aplicación, pulsa el botón "Server" para conectarte al servidor online
   - Conecta varios dispositivos y verifica que todos pueden verse entre sí

6. **Prueba del zombie en la cafetería**:
   - Navega hasta el mapa de la cafetería
   - Observa cómo el zombie se mueve e intenta perseguirte
   - Intenta evitar al zombie mientras exploras el área

## Dificultades encontradas y soluciones implementadas

### 1. Integración de puntos de interés en la Salida del Metro

**Problema**: Inicialmente, los puntos de interés en el mapa de la Salida del Metro no mostraban información al pasar sobre ellos.

**Solución**: Se implementó un sistema de detección de posición que verifica constantemente si el jugador está en una coordenada especial. Cuando esto ocurre, se muestra un mensaje y se habilita la interacción mediante el botón A.

```kotlin
// Se añadió un método para verificar la posición del jugador
private fun checkPositionForMapChange(position: Pair<Int, Int>) {
    when {
        position.first == 35 && position.second == 5 -> {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ver datos del Metro", Toast.LENGTH_SHORT).show()
            }
        }
        // Más puntos de interés...
    }
}

// Se implementó la funcionalidad del botón A
private fun handleButtonAPress() {
    val position = gameState.playerPosition
    when {
        position.first == 35 && position.second == 5 -> {
            showInfoDialog("Metro", "Línea 6 del Metro - Estación Instituto del Petróleo...")
        }
        // Más interacciones...
    }
}
```

### 2. Sincronización de jugadores entre diferentes mapas

**Problema**: Los jugadores en diferentes mapas no podían verse entre sí, y la información de posición se mezclaba.

**Solución**: Se modificó el sistema de comunicación para incluir el identificador del mapa en cada mensaje de actualización. Esto permite filtrar las actualizaciones y mostrar solo los jugadores que están en el mismo mapa.

```kotlin
// Envío de posición con identificador de mapa
serverConnectionManager.sendUpdateMessage(
    playerName,
    gameState.playerPosition,
    MapMatrixProvider.MAP_SALIDAMETRO
)

// Filtrado de jugadores por mapa en el método onMessageReceived
if (map == MapMatrixProvider.MAP_SALIDAMETRO) {
    mapView.updateRemotePlayerPosition(playerId, position, map)
}
```

### 3. Persistencia de conexiones durante cambios de mapa

**Problema**: Las conexiones al servidor online se perdían al cambiar entre actividades (mapas).

**Solución**: Se implementó un sistema para preservar el estado de conexión entre transiciones de mapa, pasando la información relevante a través de los Intents y reconectando automáticamente al servidor en cada nueva actividad.

```kotlin
// Preservar estado de conexión al cambiar de mapa
val intent = Intent(this, GameplayActivity::class.java).apply {
    putExtra("PLAYER_NAME", playerName)
    putExtra("IS_SERVER", gameState.isServer)
    putExtra("IS_CONNECTED", gameState.isConnected)  // Preservar estado de conexión
    putExtra("INITIAL_POSITION", previousPosition)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}

// Reconexión automática en la nueva actividad
if (gameState.isConnected) {
    connectToOnlineServer()
}
```

### 4. Implementación de diálogos informativos

**Problema**: No existía un mecanismo para mostrar información detallada sobre los puntos de interés.

**Solución**: Se creó un método `showInfoDialog` que utiliza AlertDialog para presentar información de manera clara y estructurada:

```kotlin
private fun showInfoDialog(title: String, message: String) {
    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
    builder.show()
}
```

### 5. Integración del botón A en la interfaz

**Problema**: La interfaz original no contaba con un botón para interactuar con los puntos de interés.

**Solución**: Se añadió el botón A al layout de la actividad y se configuró su listener para manejar las interacciones:

```xml
<Button
    android:id="@+id/button_a"
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:text="A"
    android:textColor="@android:color/white"
    android:contentDescription="Button A"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    android:layout_margin="16dp"/>
```

```kotlin
findViewById<Button?>(R.id.button_a)?.setOnClickListener {
    handleButtonAPress()
}
```

### 6. Optimización del rendimiento en dispositivos de gama baja

**Problema**: La aplicación experimentaba problemas de rendimiento en dispositivos con recursos limitados, especialmente al renderizar múltiples jugadores.

**Solución**: Se implementó un sistema de renderizado optimizado que solo actualiza las posiciones cuando es necesario y limita la frecuencia de actualización para reducir la carga del procesador:

```kotlin
// Optimización de renderizado
private fun updatePlayerPosition(position: Pair<Int, Int>) {
    // Verificar si la posición ha cambiado realmente
    if (position != gameState.playerPosition) {
        runOnUiThread {
            gameState.playerPosition = position
            mapView.updateLocalPlayerPosition(position)
            
            // Verificar puntos de interés
            checkPositionForMapChange(position)
            
            // Enviar actualización solo si estamos conectados
            if (gameState.isConnected) {
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_SALIDAMETRO)
            }
        }
    }
}
```

Estas soluciones han permitido crear una experiencia de usuario fluida y coherente a través de los diferentes mapas, manteniendo las conexiones y el estado del juego durante toda la sesión.

Capturas de pantalla de algunas funciones de la aplicación:
![[Pasted image 20250313095812.png]]
![[Pasted image 20250313095830.png]]
![[Pasted image 20250313095839.png]]
![[Pasted image 20250313095850.png]]
![[Pasted image 20250313095900.png]]