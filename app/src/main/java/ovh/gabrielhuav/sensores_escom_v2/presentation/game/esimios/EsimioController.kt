package ovh.gabrielhuav.sensores_escom_v2.presentation.game.esimios

import android.os.Handler
import android.os.Looper
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider

class EsimioController(
    private val onEsimioPositionChanged: (esimioId: String, position: Pair<Int, Int>) -> Unit,
    private val onPlayerCaught: () -> Unit
) {
    private val esimios = mutableListOf<Esimio>()
    private var isGameActive = false
    private var playerPosition: Pair<Int, Int>? = null
    private var difficulty = DIFFICULTY_EASY
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    data class Esimio(
        val id: String,
        var position: Pair<Int, Int>,
        var target: String? = null
    )

    companion object {
        const val DIFFICULTY_EASY = 1
        const val DIFFICULTY_MEDIUM = 2
        const val DIFFICULTY_HARD = 3

        private const val TAG = "EsimioController"
    }

    fun startGame(selectedDifficulty: Int) {
        difficulty = selectedDifficulty
        isGameActive = true
        esimios.clear()

        // Crear esimios según dificultad
        val esimioCount = when (difficulty) {
            DIFFICULTY_EASY -> 3
            DIFFICULTY_MEDIUM -> 5
            DIFFICULTY_HARD -> 8
            else -> 3
        }

        // Posiciones iniciales de esimios (fuera de áreas bloqueadas)
        val spawnPositions = listOf(
            Pair(20, 10),
            Pair(20, 20),
            Pair(20, 30),
            Pair(25, 15),
            Pair(25, 25),
            Pair(19, 12),
            Pair(19, 22),
            Pair(19, 32)
        )

        for (i in 0 until esimioCount) {
            val spawnPos = spawnPositions[i % spawnPositions.size]
            esimios.add(
                Esimio(
                    id = "esimio_$i",
                    position = spawnPos
                )
            )
            Log.d(TAG, "Esimio $i creado en posición: $spawnPos")
        }

        startUpdateLoop()
    }

    fun stopGame() {
        isGameActive = false
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
        esimios.clear()
    }

    fun updatePlayerPosition(playerId: String, position: Pair<Int, Int>) {
        playerPosition = position
    }

    fun setEsimioPosition(esimioId: String, position: Pair<Int, Int>) {
        esimios.find { it.id == esimioId }?.position = position
    }

    private fun startUpdateLoop() {
        val updateInterval = when (difficulty) {
            DIFFICULTY_EASY -> 1500L   // 1.5 segundos
            DIFFICULTY_MEDIUM -> 1000L  // 1 segundo
            DIFFICULTY_HARD -> 600L     // 0.6 segundos
            else -> 1500L
        }

        updateRunnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    updateEsimios()
                    handler.postDelayed(this, updateInterval)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun updateEsimios() {
        val currentPlayerPos = playerPosition ?: return

        esimios.forEach { esimio ->
            // Mover esimio hacia el jugador
            moveEsimioTowardsPlayer(esimio, currentPlayerPos)

            // Notificar cambio de posición
            onEsimioPositionChanged(esimio.id, esimio.position)

            // Verificar si atrapó al jugador
            if (isPlayerCaught(esimio, currentPlayerPos)) {
                Log.d(TAG, "¡Esimio ${esimio.id} atrapó al jugador!")
                onPlayerCaught()
                return
            }
        }
    }

    private fun moveEsimioTowardsPlayer(esimio: Esimio, playerPos: Pair<Int, Int>) {
        val (currentX, currentY) = esimio.position
        val (targetX, targetY) = playerPos

        // Calcular dirección
        val dx = targetX - currentX
        val dy = targetY - currentY

        // Movimientos posibles
        val possibleMoves = mutableListOf<Pair<Int, Int>>()

        // Preferir movimiento hacia el jugador
        if (dx != 0) {
            val newX = currentX + if (dx > 0) 1 else -1
            if (isValidMove(newX, currentY)) {
                possibleMoves.add(Pair(newX, currentY))
            }
        }
        if (dy != 0) {
            val newY = currentY + if (dy > 0) 1 else -1
            if (isValidMove(currentX, newY)) {
                possibleMoves.add(Pair(currentX, newY))
            }
        }

        // Si no hay movimientos directos, intentar todas las direcciones
        if (possibleMoves.isEmpty()) {
            val allMoves = listOf(
                Pair(currentX + 1, currentY),
                Pair(currentX - 1, currentY),
                Pair(currentX, currentY + 1),
                Pair(currentX, currentY - 1)
            )
            possibleMoves.addAll(allMoves.filter { isValidMove(it.first, it.second) })
        }

        // Elegir movimiento
        if (possibleMoves.isNotEmpty()) {
            val chosenMove = if (difficulty >= DIFFICULTY_MEDIUM) {
                // Elegir el que más se acerque al jugador
                possibleMoves.minByOrNull { move ->
                    calculateDistance(move, playerPos)
                } ?: possibleMoves.first()
            } else {
                // Movimiento aleatorio entre válidos
                possibleMoves.random()
            }
            esimio.position = chosenMove
        }
    }

    private fun isValidMove(x: Int, y: Int): Boolean {
        // Verificar límites
        if (x < 0 || x >= 40 || y < 0 || y >= 40) return false

        // Verificar que no esté en áreas bloqueadas (usando las mismas que definiste)
        val blockedAreas = listOf(
            // Edificios
            Pair(7..14, 28..29),
            Pair(16..17, 28..29),
            Pair(7..14, 31..32),
            Pair(7..14, 22..23),
            Pair(16..17, 22..23),
            Pair(7..14, 25..26),
            Pair(7..14, 15..16),
            Pair(16..17, 15..16),
            Pair(7..14, 18..19),
            Pair(7..14, 9..10),
            Pair(16..17, 9..10),
            Pair(7..14, 12..13),
            Pair(7..14, 3..4),
            Pair(16..17, 3..4),
            Pair(7..14, 6..7),
            // Pastos
            Pair(7..38, 34..38),
            Pair(32..38, 29..38),
            Pair(24..29, 6..18),
            // Zona inaccesible
            Pair(7..38, 1..4)
        )

        return !blockedAreas.any { (xRange, yRange) ->
            x in xRange && y in yRange
        }
    }

    private fun calculateDistance(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Double {
        val dx = pos1.first - pos2.first
        val dy = pos1.second - pos2.second
        return Math.sqrt((dx * dx + dy * dy).toDouble())
    }

    private fun isPlayerCaught(esimio: Esimio, playerPos: Pair<Int, Int>): Boolean {
        val distance = calculateDistance(esimio.position, playerPos)
        return distance <= 1.5 // Radio de captura
    }

    fun getEsimios(): List<Esimio> = esimios.toList()
}