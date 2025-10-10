package ovh.gabrielhuav.sensores_escom_v2.presentation.game.rabbit

import android.os.Handler
import android.os.Looper
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Controlador para gestionar los conejos en el minijuego de la ENCB.
 * Los conejos huyen de los jugadores y deben ser atrapados.
 */
class ENCBRabbitController(
    private val onRabbitPositionChanged: (String, Pair<Int, Int>) -> Unit,
    private val onRabbitCaught: (String) -> Unit
) {
    companion object {
        private const val TAG = "RabbitController"

        // Niveles de dificultad
        const val DIFFICULTY_EASY = 1
        const val DIFFICULTY_MEDIUM = 2
        const val DIFFICULTY_HARD = 3

        // Número de conejos por dificultad
        private val RABBITS_COUNT = mapOf(
            DIFFICULTY_EASY to 3,
            DIFFICULTY_MEDIUM to 5,
            DIFFICULTY_HARD to 7
        )

        // Velocidad de conejos por dificultad (ms entre movimientos - menor = más rápido)
        private val RABBIT_SPEED = mapOf(
            DIFFICULTY_EASY to 1200L,
            DIFFICULTY_MEDIUM to 800L,
            DIFFICULTY_HARD to 500L
        )

        // Rango de detección por dificultad (qué tan lejos ven a los jugadores para huir)
        val RABBIT_DETECTION_RANGE = mapOf(
            DIFFICULTY_EASY to 5,
            DIFFICULTY_MEDIUM to 8,
            DIFFICULTY_HARD to 12
        )
    }

    // Lista de conejos activos
    private val rabbits = mutableListOf<Rabbit>()

    // Lista de jugadores en el mapa (id -> posición)
    private val players = mutableMapOf<String, Pair<Int, Int>>()

    // Estado del juego
    private var isGameActive = false
    private var currentDifficulty = DIFFICULTY_EASY
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val catchDistance = 2 // Distancia para considerar que un jugador ha atrapado a un conejo

    /**
     * Clase interna para representar un conejo
     */
    inner class Rabbit(
        val id: String,
        var position: Pair<Int, Int>,
        var speed: Long = RABBIT_SPEED[DIFFICULTY_EASY]!!,
        var detectionRange: Int = RABBIT_DETECTION_RANGE[DIFFICULTY_EASY]!!,
        var isCaught: Boolean = false
    ) {
        var lastMove = System.currentTimeMillis()
        var threatPlayerId: String? = null // El jugador del que está huyendo
        var lastValidPosition = position // Guardar la última posición válida
        private var respawnTime = 0L // Tiempo de respawn después de ser atrapado

        fun move() {
            // Si el conejo fue atrapado, verificar si es tiempo de respawnear
            if (isCaught) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - respawnTime >= 2000L) { // 2 segundos de respawn
                    respawn()
                }
                return
            }

            // Solo moverse si ha pasado suficiente tiempo desde el último movimiento
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMove < speed) {
                return
            }

            lastMove = currentTime

            // Buscar jugador más cercano (amenaza)
            findNearestThreat()

            // Si hay un jugador cerca, huir
            if (threatPlayerId != null && players.containsKey(threatPlayerId)) {
                val threatPosition = players[threatPlayerId]!!
                fleeFromThreat(threatPosition)
            } else {
                // Movimiento aleatorio si no hay amenazas
                moveRandomly()
            }

            // CRÍTICO: Notificar cambio de posición SIEMPRE
            onRabbitPositionChanged(id, position)
        }

        private fun findNearestThreat() {
            if (players.isEmpty()) {
                threatPlayerId = null
                return
            }

            var nearestPlayer: String? = null
            var shortestDistance = Double.MAX_VALUE

            players.forEach { (playerId, playerPos) ->
                val distance = calculateDistance(position, playerPos)

                if (distance < shortestDistance && distance <= detectionRange) {
                    shortestDistance = distance
                    nearestPlayer = playerId
                }
            }

            // Actualizar la amenaza si encontramos un jugador cerca
            threatPlayerId = if (nearestPlayer != null) {
                nearestPlayer
            } else {
                // Si no hay amenazas, limpiar el objetivo
                null
            }
        }

        private fun fleeFromThreat(threatPosition: Pair<Int, Int>) {
            // Calcular dirección opuesta al jugador (huir)
            val dx = position.first - threatPosition.first
            val dy = position.second - threatPosition.second

            // Guardar la posición actual como válida antes de intentar moverse
            lastValidPosition = position

            // Crear una lista de posibles movimientos priorizados (alejarse del jugador)
            val possibleMoves = mutableListOf<Pair<Int, Int>>()

            // Decidir qué dirección tiene prioridad (horizontal o vertical)
            if (abs(dx) > abs(dy)) {
                // Priorizar movimiento horizontal (alejarse)
                addMove(possibleMoves, position.first + if (dx > 0) 1 else -1, position.second)
                addMove(possibleMoves, position.first, position.second + if (dy > 0) 1 else -1)
                addMove(possibleMoves, position.first, position.second + if (dy > 0) -1 else 1)
                // Último recurso: acercarse (solo si no hay otra opción)
                addMove(possibleMoves, position.first + if (dx > 0) -1 else 1, position.second)
            } else {
                // Priorizar movimiento vertical (alejarse)
                addMove(possibleMoves, position.first, position.second + if (dy > 0) 1 else -1)
                addMove(possibleMoves, position.first + if (dx > 0) 1 else -1, position.second)
                addMove(possibleMoves, position.first + if (dx > 0) -1 else 1, position.second)
                // Último recurso: acercarse
                addMove(possibleMoves, position.first, position.second + if (dy > 0) -1 else 1)
            }

            // En dificultad difícil, añadir posibilidad de movimiento diagonal
            if (currentDifficulty == DIFFICULTY_HARD && Random.nextDouble() < 0.4) {
                addMove(
                    possibleMoves,
                    position.first + if (dx > 0) 1 else -1,
                    position.second + if (dy > 0) 1 else -1
                )
            }

            // Intentar cada movimiento hasta encontrar uno válido
            for (newPosition in possibleMoves) {
                if (isValidPosition(newPosition.first, newPosition.second)) {
                    position = newPosition
                    break
                }
            }

            // Si no se ha movido (por estar rodeado de obstáculos), mantener la última posición válida
            if (position == lastValidPosition) {
                moveRandomly() // Intentar moverse aleatoriamente si está bloqueado
            }
        }

        private fun addMove(list: MutableList<Pair<Int, Int>>, x: Int, y: Int) {
            val newPosition = Pair(x.coerceIn(0, 39), y.coerceIn(0, 39))
            if (!list.contains(newPosition)) {
                list.add(newPosition)
            }
        }

        private fun moveRandomly() {
            // Crear una lista de posibles movimientos aleatorios
            val possibleDirections = mutableListOf<Pair<Int, Int>>()

            // Añadir las cuatro direcciones cardinales
            possibleDirections.add(Pair(position.first + 1, position.second)) // Derecha
            possibleDirections.add(Pair(position.first - 1, position.second)) // Izquierda
            possibleDirections.add(Pair(position.first, position.second + 1)) // Abajo
            possibleDirections.add(Pair(position.first, position.second - 1)) // Arriba

            // Filtrar solo movimientos válidos
            val validMoves = possibleDirections.filter { pos ->
                isValidPosition(pos.first, pos.second)
            }

            // Si hay movimientos válidos, elegir uno al azar
            if (validMoves.isNotEmpty()) {
                lastValidPosition = position
                position = validMoves.random()
            }
            // Si no hay movimientos válidos, el conejo se queda en su posición actual
        }

        fun catchRabbit() {
            isCaught = true
            respawnTime = System.currentTimeMillis()
            Log.d(TAG, "Conejo $id atrapado, respawneará en 2 segundos")
        }

        private fun respawn() {
            // Encontrar una nueva posición de spawn válida
            var xPos: Int
            var yPos: Int
            var attempts = 0
            do {
                xPos = Random.nextInt(5, 35)
                yPos = Random.nextInt(5, 35)
                attempts++
                if (attempts > 100) {
                    xPos = 20
                    yPos = 20
                    break
                }
            } while (!isValidPosition(xPos, yPos))

            position = Pair(xPos, yPos)
            lastValidPosition = position
            isCaught = false

            // CRÍTICO: Notificar nueva posición después de respawn
            onRabbitPositionChanged(id, position)

            Log.d(TAG, "Conejo $id respawneado en $position")
        }
    }

    /**
     * Inicia el minijuego con una dificultad específica
     */
    fun startGame(difficulty: Int = DIFFICULTY_EASY) {
        if (isGameActive) return

        isGameActive = true
        currentDifficulty = difficulty.coerceIn(DIFFICULTY_EASY, DIFFICULTY_HARD)

        // Crear conejos según la dificultad
        createRabbits()

        // Iniciar el bucle de actualización
        startUpdateLoop()

        Log.d(TAG, "Minijuego de conejos iniciado con dificultad $difficulty y ${rabbits.size} conejos")
    }

    /**
     * Detiene el minijuego
     */
    fun stopGame() {
        if (!isGameActive) return

        isGameActive = false

        // Detener el bucle de actualización
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null

        // Limpiar conejos
        rabbits.clear()

        Log.d(TAG, "Minijuego de conejos detenido")
    }

    /**
     * Actualiza la posición de un jugador
     */
    fun updatePlayerPosition(playerId: String, position: Pair<Int, Int>) {
        players[playerId] = position
    }

    /**
     * Elimina un jugador de la lista
     */
    fun removePlayer(playerId: String) {
        players.remove(playerId)
    }

    /**
     * Intenta atrapar un conejo cerca de la posición del jugador
     * Retorna el ID del conejo atrapado o null si no hay ninguno cerca
     */
    fun tryToCatchRabbit(playerPosition: Pair<Int, Int>): String? {
        for (rabbit in rabbits) {
            if (!rabbit.isCaught) {
                val distance = calculateDistance(playerPosition, rabbit.position)

                if (distance <= catchDistance) {
                    // ¡Conejo atrapado!
                    rabbit.catchRabbit()
                    onRabbitCaught(rabbit.id)
                    Log.d(TAG, "¡Conejo ${rabbit.id} atrapado por jugador!")
                    return rabbit.id
                }
            }
        }

        return null
    }

    private fun createRabbits() {
        rabbits.clear()

        val count = RABBITS_COUNT[currentDifficulty] ?: 3
        val speed = RABBIT_SPEED[currentDifficulty] ?: 1200L
        val range = RABBIT_DETECTION_RANGE[currentDifficulty] ?: 5

        Log.d(TAG, "🐰 Creando $count conejos con dificultad $currentDifficulty")

        for (i in 0 until count) {
            var xPos: Int
            var yPos: Int
            var attempts = 0
            do {
                xPos = Random.nextInt(10, 30)
                yPos = Random.nextInt(10, 30)
                attempts++
                if (attempts > 100) {
                    Log.w(TAG, "Usando posición fija para conejo $i")
                    xPos = 20 + i // Distribuir si hay múltiples conejos
                    yPos = 20
                    break
                }
            } while (!isValidPosition(xPos, yPos))

            val rabbit = Rabbit(
                id = "rabbit_$i",
                position = Pair(xPos, yPos),
                speed = speed,
                detectionRange = range
            )

            rabbits.add(rabbit)

            Log.d(TAG, "🐰 Conejo ${rabbit.id} creado en posición ${rabbit.position}")

            // CRÍTICO: Notificar posición inicial INMEDIATAMENTE
            // Este es el callback que hace que se visualice el conejo
            onRabbitPositionChanged(rabbit.id, rabbit.position)
        }

        Log.d(TAG, "✅ Total de conejos creados: ${rabbits.size}")
    }

    private fun startUpdateLoop() {
        // Detener el bucle existente si lo hay
        updateRunnable?.let { handler.removeCallbacks(it) }

        // Crear nuevo bucle
        val runnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    try {
                        updateRabbits()
                        handler.postDelayed(this, 100) // Actualizar más rápido que el movimiento individual
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en bucle de actualización: ${e.message}")
                        handler.postDelayed(this, 500) // Reintentar después de un tiempo
                    }
                }
            }
        }

        updateRunnable = runnable
        handler.post(runnable)
    }

    private fun updateRabbits() {
        rabbits.forEach { it.move() }
    }

    /**
     * Establece manualmente la posición de un conejo (para sincronización)
     */
    fun setRabbitPosition(rabbitId: String, position: Pair<Int, Int>) {
        // Solo aceptar posiciones válidas
        if (!isValidPosition(position.first, position.second)) {
            Log.d(TAG, "Posición inválida para conejo: $position")
            return
        }

        val rabbit = rabbits.find { it.id == rabbitId }

        if (rabbit != null) {
            rabbit.position = position
            rabbit.lastValidPosition = position
        } else if (isGameActive) {
            // Si no existe el conejo pero el juego está activo, crear uno nuevo
            val newRabbit = Rabbit(
                id = rabbitId,
                position = position,
                speed = RABBIT_SPEED[currentDifficulty]!!,
                detectionRange = RABBIT_DETECTION_RANGE[currentDifficulty]!!
            )
            rabbits.add(newRabbit)
        }
    }

    /**
     * Verifica si una posición es válida (no es una pared u obstáculo)
     */
    private fun isValidPosition(x: Int, y: Int): Boolean {
        // Verificar límites del mapa
        if (x < 0 || x >= MapMatrixProvider.MAP_WIDTH || y < 0 || y >= MapMatrixProvider.MAP_HEIGHT) {
            return false
        }

        try {
            // Obtener la matriz de la ENCB directamente del proveedor
            val encbMatrix = MapMatrixProvider.getMatrixForMap(MapMatrixProvider.MAP_ENCB)

            // Verificar colisiones con la matriz del mapa
            val cellType = encbMatrix[y][x]

            // Los conejos pueden moverse por caminos (PATH=2) y áreas interactivas (INTERACTIVE=0)
            // pero no por paredes (WALL=1) ni obstáculos (INACCESSIBLE=3)
            return cellType == MapMatrixProvider.PATH || cellType == MapMatrixProvider.INTERACTIVE
        } catch (e: Exception) {
            Log.e(TAG, "Error accediendo a la matriz: ${e.message}")
            // Si hay un error, consideramos que la posición no es válida por seguridad
            return false
        }
    }

    /**
     * Calcula la distancia entre dos puntos
     */
    private fun calculateDistance(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Double {
        val dx = pos1.first - pos2.first
        val dy = pos1.second - pos2.second
        return sqrt((dx * dx + dy * dy).toDouble())
    }

    /**
     * Devuelve la lista de conejos activos y sus posiciones
     */
    fun getRabbits(): List<Pair<String, Pair<Int, Int>>> {
        return rabbits.filter { !it.isCaught }.map { it.id to it.position }
    }

    /**
     * Devuelve el número de conejos que aún no han sido atrapados
     */
    fun getActiveRabbitCount(): Int {
        return rabbits.count { !it.isCaught }
    }

    /**
     * Devuelve el nivel de dificultad actual
     */
    fun getDifficulty(): Int {
        return currentDifficulty
    }
}