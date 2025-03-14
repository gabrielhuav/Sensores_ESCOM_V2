# 📘 Proyecto: Navegación y Sincronización Multijugador en Mapas Interactivos

## 📝 **Descripción**
Este proyecto tiene como objetivo desarrollar una aplicación móvil para Android que permita la interacción multijugador mediante sincronización de datos entre dispositivos conectados vía Bluetooth y un servidor Node.js. Además, incorpora sensores móviles para enriquecer la experiencia de navegación en un mapa interactivo de la ESCOM.

---

## 🚀 **Características Principales**

### **📡 Sistema Bluetooth**
- Comunicación bidireccional entre dispositivos cercanos.
- Retransmisión de posiciones locales entre cliente y servidor Bluetooth.
- Sincronización inicial con un servidor Node.js.

### **🌐 Sincronización con Node.js**
- El servidor Node.js recibe y actualiza las posiciones globales.
- Los jugadores conectados al servidor pueden visualizar las posiciones de otros jugadores.

### **🗺️ Mapa Interactivo**
- Cambios dinámicos entre mapas al alcanzar coordenadas específicas.
- Implementación inicial de una transición a un mapa interior al ingresar a un edificio.

### **📲 Sensores del Dispositivo**
- Uso de Bluetooth para conexión directa entre jugadores cercanos.
- Geolocalización y giroscopio para navegación y eventos en el mapa.

---

## ⚙️ **Configuración del Proyecto**

### **Requisitos Previos**
- **Software:**
  - Android Studio.
  - Node.js.
- **Hardware:**
  - Dispositivo Android con soporte para Bluetooth.
  - Permisos de ubicación y Bluetooth habilitados.
- **Backend:**
  - Servidor Node.js con WebSocket activo.

### **Instrucciones de Configuración**
1. Clonar el repositorio:
2. Configurar el proyecto de Android Studio
3. Cambiar la dirección IP del del proyecto en Android Studio para que coincida con la del servidor Node.js
4. Abrir el proyecto Android en Android Studio y ejecutar en un dispositivo físico.


### 🛠️ **Trabajo Futuro**
- **Sincronización Completa:**
  - Bluetooth: Retransmitir posiciones globales desde el servidor Bluetooth al cliente Bluetooth..
  - Node.js: Actualizar la lógica para sincronizar datos locales y remotos.
- **Manejo de Mapas Múltiples:**
  - Manejo de Mapas Múltiples
  - Persistencia de conexiones durante cambios entre actividades.
- **Mejoras en los Sensoress:**
  - Integración de sensores adicionales como acelerómetro y cámara.
  - Reconocimiento de voz y escaneo de códigos QR.


Este código es parte de un juego desarrollado para la Escuela Superior de Cómputo (ESCOM) que combina elementos de geolocalización, Bluetooth y conexiones en línea para crear una experiencia interactiva y colaborativa. El objetivo principal del juego es permitir a los jugadores moverse por un mapa virtual que representa la ESCOM, interactuar con otros jugadores y realizar acciones específicas en ciertas ubicaciones.

### Características principales:

1. **Movimiento en el Mapa**:
   - Los jugadores pueden moverse por un mapa virtual que representa la ESCOM utilizando botones de dirección (norte, sur, este, oeste).
   - La posición del jugador se actualiza en tiempo real y se sincroniza con otros jugadores a través de Bluetooth o un servidor en línea.

2. **Interacción con el Entorno**:
   - El juego detecta cuando un jugador está en una posición específica (por ejemplo, frente a un edificio) y permite realizar acciones como entrar al edificio o salir de la ESCOM.
   - Estas interacciones están condicionadas por la posición del jugador en el mapa.

3. **Conexión Bluetooth y en Línea**:
   - Los jugadores pueden conectarse entre sí mediante Bluetooth para compartir su posición y estado en el juego.
   - También existe la opción de conectarse a un servidor en línea para sincronizar las posiciones y acciones de todos los jugadores en tiempo real.

4. **Roles de Jugador**:
   - **Servidor**: Un jugador puede actuar como servidor, gestionando la conexión Bluetooth y sincronizando las posiciones de los demás jugadores.
   - **Cliente**: Los jugadores pueden conectarse al servidor para recibir actualizaciones de las posiciones de otros jugadores.

5. **Cambio de Mapa**:
   - Cuando un jugador llega a ciertas ubicaciones, puede cambiar a otro mapa (por ejemplo, entrar a un edificio) o salir de la ESCOM, lo que lleva a una nueva actividad o escenario.

6. **Persistencia de Estado**:
   - El juego guarda el estado actual (posición del jugador, conexiones Bluetooth, etc.) para que los jugadores puedan continuar donde lo dejaron incluso si la aplicación se cierra o se reinicia.


### Dificultades en el desarrollo
- Una de las dificultades mas grandes fue adapartnos al codigo ya implementado debido a las muchas funcionalidades ya implementadas
- El realizar un nuevo mapa con puntos interactivos fue un punto algo desafiante pues teniamos que acomodarlos correcatmente con la imagen que elegimos