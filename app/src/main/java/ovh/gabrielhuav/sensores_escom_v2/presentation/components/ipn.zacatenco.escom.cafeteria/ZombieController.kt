package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.cafeteria

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Controlador para gestionar el zombie en el minijuego de la cafetería
 */
class ZombieController(
    private val onZombiePositionChanged: (Pair<Int, Int>) -> Unit,
    private val onPlayerCaught: () -> Unit
) {
    // Posición actual del zombie
    private var position = Pair(30, 30) // Posición inicial en un extremo de la cafetería

    // Velocidad del zombie (menor número = más rápido)
    private var updateDelayMs = 1000L

    // Handler para actualizar la posición periódicamente
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    // Estado del minijuego
    private var isGameActive = false
    private var playerPosition = Pair(1, 1)
    private var difficulty = 1 // 1-3, donde 3 es más difícil

    // Distancia para considerar que el zombie ha atrapado al jugador
    private val catchDistance = 2

    // Pinta para dibujar el zombie
    private val zombiePaint = Paint().apply {
        color = Color.rgb(50, 150, 50) // Verde zombie
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3f
    }

    private val zombieTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    /**
     * Inicia el minijuego del zombie
     */
    fun startGame(initialDifficulty: Int = 1) {
        isGameActive = true
        difficulty = initialDifficulty.coerceIn(1, 3)

        // Ajustar velocidad según dificultad
        updateDelayMs = when (difficulty) {
            1 -> 1200L // Fácil
            2 -> 800L  // Medio
            else -> 500L // Difícil
        }

        // Colocar al zombie en una posición aleatoria lejana al jugador
        resetZombiePosition()

        // Iniciar el loop de actualización
        startUpdateLoop()

        Log.d(TAG, "Minijuego zombie iniciado con dificultad $difficulty")
    }

    /**
     * Detiene el minijuego
     */
    fun stopGame() {
        isGameActive = false
        stopUpdateLoop()
        Log.d(TAG, "Minijuego zombie detenido")
    }

    /**
     * Actualiza la posición del jugador
     */
    fun updatePlayerPosition(position: Pair<Int, Int>) {
        playerPosition = position
        checkForCollision()
    }

    /**
     * Dibuja el zombie en el canvas
     */
    fun drawZombie(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        if (!isGameActive) return

        val zombieX = position.first * cellWidth + cellWidth / 2
        val zombieY = position.second * cellHeight + cellHeight / 2

        // Dibujar el cuerpo del zombie (más grande que un jugador normal)
        canvas.drawCircle(zombieX, zombieY, cellWidth * 0.4f, zombiePaint)

        // Dibujar texto "ZOMBIE"
        canvas.drawText("ZOMBIE", zombieX, zombieY - cellHeight * 0.7f, zombieTextPaint)
    }

    /**
     * Verifica si el zombie ha atrapado al jugador
     */
    private fun checkForCollision() {
        if (!isGameActive) return

        val distanceX = abs(position.first - playerPosition.first)
        val distanceY = abs(position.second - playerPosition.second)

        // Si el zombie está suficientemente cerca del jugador
        if (distanceX <= catchDistance && distanceY <= catchDistance) {
            Log.d(TAG, "¡Jugador atrapado por el zombie!")
            onPlayerCaught()
            stopGame()
        }
    }

    /**
     * Restablece la posición del zombie a un punto aleatorio lejos del jugador
     */
    private fun resetZombiePosition() {
        // Generar una posición alejada del jugador
        var newX: Int
        var newY: Int

        do {
            newX = Random.nextInt(5, 35)
            newY = Random.nextInt(5, 35)
        } while (abs(newX - playerPosition.first) < 15 || abs(newY - playerPosition.second) < 15)

        position = Pair(newX, newY)
        onZombiePositionChanged(position)
    }

    /**
     * Inicia el bucle de actualización
     */
    private fun startUpdateLoop() {
        stopUpdateLoop() // Asegurar que no haya bucles duplicados

        updateRunnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    updateZombiePosition()
                    handler.postDelayed(this, updateDelayMs)
                }
            }
        }

        updateRunnable?.let { handler.post(it) }
    }

    /**
     * Detiene el bucle de actualización
     */
    private fun stopUpdateLoop() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }

    /**
     * Actualiza la posición del zombie para que persiga al jugador
     */
    private fun updateZombiePosition() {
        if (!isGameActive) return

        val currentX = position.first
        val currentY = position.second
        val targetX = playerPosition.first
        val targetY = playerPosition.second

        // Calcular dirección para acercarse al jugador
        val moveX = if (currentX < targetX) 1 else if (currentX > targetX) -1 else 0
        val moveY = if (currentY < targetY) 1 else if (currentY > targetY) -1 else 0

        // En dificultades más altas, el zombie puede moverse en diagonal
        val newPosition = if (difficulty >= 2 && Random.nextBoolean()) {
            // Movimiento en ambas direcciones (diagonal)
            Pair(currentX + moveX, currentY + moveY)
        } else {
            // Movimiento en una sola dirección (horizontal o vertical)
            if (Random.nextBoolean() && moveX != 0) {
                Pair(currentX + moveX, currentY)
            } else if (moveY != 0) {
                Pair(currentX, currentY + moveY)
            } else {
                // Si no hay movimiento posible, mantener posición
                position
            }
        }

        // Actualizar posición
        position = newPosition

        // Verificar si el zombie atrapó al jugador
        checkForCollision()

        // Notificar cambio de posición
        onZombiePositionChanged(position)
    }

    /**
     * Establece la posición del zombie (usado para sincronizar con el servidor)
     */
    fun setZombiePosition(position: Pair<Int, Int>) {
        this.position = position
        checkForCollision()
    }

    /**
     * Procesa mensajes del servidor WebSocket relacionados con el zombie
     */
    fun processServerMessage(message: JSONObject) {
        try {
            when (message.getString("type")) {
                "zombie_position" -> {
                    val x = message.getInt("x")
                    val y = message.getInt("y")
                    setZombiePosition(Pair(x, y))
                }
                "zombie_difficulty" -> {
                    val newDifficulty = message.getInt("level")
                    difficulty = newDifficulty.coerceIn(1, 3)
                    updateDelayMs = when (difficulty) {
                        1 -> 1200L
                        2 -> 800L
                        else -> 500L
                    }
                }
                "zombie_game_start" -> {
                    val gameDifficulty = message.optInt("difficulty", 1)
                    startGame(gameDifficulty)
                }
                "zombie_game_stop" -> {
                    stopGame()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje del servidor: ${e.message}")
        }
    }

    /**
     * Genera un mensaje para enviar al servidor con la posición del zombie
     */
    fun generateZombiePositionMessage(): String {
        return JSONObject().apply {
            put("type", "zombie_position")
            put("x", position.first)
            put("y", position.second)
            put("map", "escom_cafeteria")
        }.toString()
    }

    companion object {
        private const val TAG = "ZombieController"
    }
}