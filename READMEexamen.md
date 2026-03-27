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

# 📝 Examen
## *“Área deportiva e Implementación y Lógica de Minijuegos Interactivos”*

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
**27/03/2026**

---
## General

Se implemento las áreas deportivas, siendo estas las canchas de IA y las de gestión escolar. Para este proceso sucedieron 2 cosas distintas, siendo que en canchas de IA aunque ya tenia la información, al interactuar te sacaba del juego, por lo que se llevo un proceso de análisis para evitar que esto surgiera, a su vez que se hizo el cambio respectivo para que funcionara el minijuego. Por otra parte, las canchas de gestión se realizo desde 0, por lo que llevo la creación de un fondo y la programación pertinente para que funcionara igual que los otros escenarios.

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
<summary><b>Antes del cambio</b></summary>
<br>
<p align="center">
<img width="921" height="414" alt="image" src="https://github.com/user-attachments/assets/5a42d561-fab3-4b64-8588-9431852c9b7f" />
<img width="921" height="414" alt="image" src="https://github.com/user-attachments/assets/a91f6b50-c1d7-48eb-823d-1aa8a835b88d" />
<img width="921" height="414" alt="image" src="https://github.com/user-attachments/assets/065d9a58-364f-469d-8bed-549658ead599" />
</p>
</details>

<details>
<summary><b>Despues del cambio</b></summary>
<br>
<p align="center">
<img width="800" height="360" alt="main" src="https://github.com/user-attachments/assets/e3769bae-e828-4da7-b973-0baefbc04dbe" />
<img width="800" height="360" alt="canchasIA" src="https://github.com/user-attachments/assets/a94ebbfd-b313-4e37-a94d-5663bd5b02bf" />
<img width="800" height="360" alt="basketball" src="https://github.com/user-attachments/assets/071bc752-a332-473b-bacb-17bad83c9509" />
<img width="800" height="360" alt="Canchasgestion" src="https://github.com/user-attachments/assets/b330f6d2-943f-4d0f-a569-817baed40402" />
<img width="360" height="800" alt="futbol" src="https://github.com/user-attachments/assets/9762ea62-72c3-4d20-912a-9897d071cda2" />
<img width="800" height="360" alt="barras" src="https://github.com/user-attachments/assets/414c9f80-8a2b-4237-b357-5d088e07caed" />
</p>
</details>
