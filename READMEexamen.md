# 🏛️ INSTITUTO POLITÉCNICO NACIONAL

<details>
<summary>🖼️ <b>Ver Logo IPN</b></summary>
<br>
<p align="center">
  <img width="330" height="185" alt="Logo IPN" src="https://github.com/user-attachments/assets/9f09e6a1-45d3-4025-8081-c64e6ad5b44e" />
</p>
</details>

## 💻 ESCUELA SUPERIOR DE CÓMPUTO

<details>
<summary>🖼️ <b>Ver Logo ESCOM</b></summary>
<br>
<p align="center">
  <img width="198" height="152" alt="Logo ESCOM" src="https://github.com/user-attachments/assets/79161b91-c36f-4462-ae72-6175fa87abe8" />
</p>
</details>

### 📱 UNIDAD ACADÉMICA:
> **Desarrollo de aplicaciones móviles nativas**

---

# 📝 PRÁCTICA
## *“Implementación y Lógica de Minijuegos Interactivos”*

---

### 👨‍🏫 PROFESOR:
**GABRIEL HURTADO AVILES**

### 👥 GRUPO:
**7CV4**

### 👨‍💻 ALUMNOS:
* **Blanco López Juan Antonio**
* **Carmona Martínez Ricardo**
* **Muciño Torres Diego Ivan**

### 📅 FECHA DE ENTREGA:
**[Ingresar Fecha]**

---

## 🎮 Funcionamiento de los Minijuegos

En esta fase del proyecto, modificamos la aplicación para incluir tres zonas interactivas en el mapa. Cada zona activa un minijuego diferente con sus propias mecánicas y reglas.

### 🏀 1. Minijuego de Básquetbol (Cancha de IA)
Se trata de un juego de precisión donde el usuario debe calcular el tiro perfecto para encestar. Se abre como una ventana flotante sobre el mapa para no interrumpir la navegación principal.

* **Archivos clave:** `BasketballGame.kt`, `TrajectoryView.kt`, `dialog_basketball.xml`.
* **¿Cómo se juega?:** 1. El juego tiene dos barras de progreso: una para el **Ángulo** y otra para la **Potencia**.
  2. Las barras suben y bajan rápidamente. El jugador presiona "FIJAR" para detener la barra de ángulo, y luego "LANZAR" para detener la de potencia.
  3. Dependiendo de dónde se detuvieron las barras, el sistema dibuja una línea punteada y lanza el balón siguiendo esa curva.
  4. Si la posición final del balón coincide con el aro, es canasta. Si toca los bordes, rebota (falla).

### ⚽ 2. Minijuego de Penales (Canchas de Gestión)
Un juego de reflejos rápidos y suerte, donde te enfrentas a un portero automático. Ocurre en una pantalla completa dedicada a este evento.

* **Archivos clave:** `PenalesActivity.kt`, `penales.xml`.
* **¿Cómo se juega?:**
  1. Tienes un límite de 7 tiros.
  2. El jugador desliza el dedo por la pantalla (*swipe/fling*) hacia la izquierda, hacia la derecha, o hacia arriba (centro) para decidir la dirección del disparo.
  3. Al mismo tiempo que el jugador tira, el portero elige una dirección al azar para lanzarse.
  4. Si el portero elige el mismo lado que el jugador, el tiro es atajado. Si elige un lado distinto, es ¡Gol!
  5. Al terminar los 7 tiros, si metiste 4 o más, ganas.

### 🏋️ 3. Minijuego de Pesas (Gimnasio)
Un minijuego de *timing* (ritmo) que pone a prueba la sincronización del usuario. Se incrusta directamente en la vista a través de un Fragmento.

* **Archivos clave:** `PesasFragment.kt`, `fragment_pesas.xml`.
* **¿Cómo se juega?:**
  1. El jugador tiene 3 vidas (representadas por corazones).
  2. Hay un indicador rojo que se mueve de izquierda a derecha sin parar sobre una barra. En la barra hay una zona de color verde (el objetivo).
  3. El usuario debe presionar el botón "¡Presiona!" exactamente cuando el indicador rojo esté dentro de la zona verde.
  4. Al acertar 5 veces, subes de nivel: la velocidad aumenta y la zona verde se hace más pequeña.
  5. Si fallas, pierdes un corazón y la pesa se cae. Si pierdes los 3 corazones, es *Game Over*.

---

## 📸 Capturas de Pantalla

<details>
<summary><b>Ver Galería de Minijuegos</b></summary>
<br>
<p align="center">
  </p>
</details>
