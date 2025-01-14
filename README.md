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


### IMPLEMENTACIÓN DE QR###
Archivos principales
1. GameplayActivity.kt
Maneja la lógica principal del juego: movimiento, interacción con celdas QR y sincronización de posiciones.

2. SiteDetailActivity.kt
Muestra información detallada sobre una ubicación específica basada en la coordenada QR.

3. activity_gameplay.xml
Archivo de diseño para la interfaz de la actividad principal, incluyendo el mapa y los botones de navegación.

### Personalización
Cambiar coordenadas QR
Modifica la lista qrCells en GameplayActivity:
private val qrCells = listOf(Pair(1,1), Pair(11, 9), Pair(17, 17), Pair(2,17), Pair(5,9))

### Añadir nuevas imágenes para celdas QR
Agrega nuevas imágenes a la carpeta res/drawable.
Actualiza el mapa siteData para asociar coordenadas a las imágenes:

private val siteData = mapOf(
    Pair(16, 4) to R.drawable.new_image,
    Pair(11, 9) to R.drawable.another_image
)


