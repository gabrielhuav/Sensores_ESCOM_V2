package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.cafeteria

import android.os.Handler
import android.os.Looper
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Controlador para gestionar los zombies en el minijuego de la cafetería
 */
class ZombieController(
    private val onZombiePositionChanged: (String, Pair<Int, Int>) -> Unit,
    private val onPlayerCaught: () -> Unit
) {
    companion object {
        private const val TAG = "ZombieController"

        // Niveles de dificultad
        const val DIFFICULTY_EASY = 1
        const val DIFFICULTY_MEDIUM = 2
        const val DIFFICULTY_HARD = 3

        // Número de zombies por dificultad
        private val ZOMBIES_COUNT = mapOf(
            DIFFICULTY_EASY to 2,
            DIFFICULTY_MEDIUM to 4,
            DIFFICULTY_HARD to 6
        )

        // Velocidad de zombies por dificultad (ms entre movimientos - menor = más rápido)
        private val ZOMBIE_SPEED = mapOf(
            DIFFICULTY_EASY to 1200L,
            DIFFICULTY_MEDIUM to 800L,
            DIFFICULTY_HARD to 500L
        )

        // Rango de detección por dificultad
        val ZOMBIE_DETECTION_RANGE = mapOf(
            DIFFICULTY_EASY to 8,
            DIFFICULTY_MEDIUM to 12,
            DIFFICULTY_HARD to 18
        )
    }

    // Lista de zombies activos
    private val zombies = mutableListOf<Zombie>()

    // Lista de jugadores en el mapa (id -> posición)
    private val players = mutableMapOf<String, Pair<Int, Int>>()

    // Estado del juego
    private var isGameActive = false
    private var currentDifficulty = DIFFICULTY_EASY
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val catchDistance = 2 // Distancia para considerar que un zombie ha atrapado a un jugador

    private lateinit var fogOfWarRenderer: FogOfWarRenderer
    private var fogOfWarEnabled = false // Flag para activar/desactivar la niebla

    /**
     * Clase interna para representar un zombie
     */
    inner class Zombie(
        val id: String,
        var position: Pair<Int, Int>,
        var speed: Long = ZOMBIE_SPEED[DIFFICULTY_EASY]!!,
        var detectionRange: Int = ZOMBIE_DETECTION_RANGE[DIFFICULTY_EASY]!!
    ) {
        var lastMove = System.currentTimeMillis()
        var targetPlayerId: String? = null
        var lastValidPosition = position // Guardar la última posición válida

        fun move() {
            // Solo moverse si ha pasado suficiente tiempo desde el último movimiento
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMove < speed) {
                return
            }

            lastMove = currentTime

            // Buscar jugador más cercano
            findNearestPlayer()

            // Si no hay jugadores, moverse aleatoriamente
            if (targetPlayerId == null || !players.containsKey(targetPlayerId)) {
                moveRandomly()
                return
            }

            // Perseguir al jugador objetivo
            val targetPosition = players[targetPlayerId]!!
            moveTowardsTarget(targetPosition)

            // Notificar cambio de posición
            onZombiePositionChanged(id, position)

            // Verificar colisiones con jugadores
            checkPlayerCollisions()
        }

        private fun findNearestPlayer() {
            if (players.isEmpty()) {
                targetPlayerId = null
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

            // Solo cambiar de objetivo si encontramos uno más cercano o no teníamos ninguno
            if (nearestPlayer != null) {
                targetPlayerId = nearestPlayer
            } else if (Random.nextDouble() < 0.1) {
                // 10% de probabilidad de cambiar a un objetivo aleatorio si no hay uno cercano
                targetPlayerId = players.keys.randomOrNull()
            }
        }

        private fun moveTowardsTarget(targetPosition: Pair<Int, Int>) {
            // Calcular dirección hacia el objetivo
            val dx = targetPosition.first - position.first
            val dy = targetPosition.second - position.second

            // Guardar la posición actual como válida antes de intentar moverse
            lastValidPosition = position

            // Crear una lista de posibles movimientos priorizados
            val possibleMoves = mutableListOf<Pair<Int, Int>>()

            // Decidir qué dirección tiene prioridad (horizontal o vertical)
            if (abs(dx) > abs(dy)) {
                // Priorizar movimiento horizontal
                addMove(possibleMoves, position.first + if (dx > 0) 1 else -1, position.second)
                addMove(possibleMoves, position.first, position.second + if (dy > 0) 1 else -1)
                addMove(possibleMoves, position.first, position.second + if (dy > 0) -1 else 1)
                addMove(possibleMoves, position.first + if (dx > 0) -1 else 1, position.second)
            } else {
                // Priorizar movimiento vertical
                addMove(possibleMoves, position.first, position.second + if (dy > 0) 1 else -1)
                addMove(possibleMoves, position.first + if (dx > 0) 1 else -1, position.second)
                addMove(possibleMoves, position.first + if (dx > 0) -1 else 1, position.second)
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
            val validMoves = possibleDirections.filter {
                    pos -> isValidPosition(pos.first, pos.second)
            }

            // Si hay movimientos válidos, elegir uno al azar
            if (validMoves.isNotEmpty()) {
                lastValidPosition = position
                position = validMoves.random()
            }
            // Si no hay movimientos válidos, el zombie se queda en su posición actual
        }

        private fun checkPlayerCollisions() {
            players.forEach { (playerId, playerPos) ->
                val distance = calculateDistance(position, playerPos)

                if (distance <= catchDistance) {
                    // ¡Zombie atrapó a un jugador!
                    Log.d(TAG, "¡Zombie $id atrapó al jugador $playerId!")
                    onPlayerCaught()
                }
            }
        }
    }

    /**
     * Inicia el minijuego con una dificultad específica
     */
    fun startGame(difficulty: Int = DIFFICULTY_EASY) {
        if (isGameActive) return

        isGameActive = true
        currentDifficulty = difficulty.coerceIn(DIFFICULTY_EASY, DIFFICULTY_HARD)

        // Crear zombies según la dificultad
        createZombies()

        // Iniciar el bucle de actualización
        startUpdateLoop()

        Log.d(TAG, "Minijuego zombie iniciado con dificultad $difficulty y ${zombies.size} zombies")
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

        // Limpiar zombies
        zombies.clear()

        Log.d(TAG, "Minijuego zombie detenido")
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
     * Ralentiza temporalmente los zombies (cuando un jugador recoge comida)
     */
    fun slowDownZombies(durationMs: Long = 3000) {
        val originalSpeeds = zombies.associate { it.id to it.speed }

        // Aumentar velocidad (más milisegundos entre movimientos = más lento)
        zombies.forEach { it.speed += 500 }

        // Restaurar velocidad después de la duración
        handler.postDelayed({
            zombies.forEach { zombie ->
                originalSpeeds[zombie.id]?.let { originalSpeed ->
                    zombie.speed = originalSpeed
                }
            }
        }, durationMs)
    }

    private fun createZombies() {
        zombies.clear()

        val count = ZOMBIES_COUNT[currentDifficulty] ?: 2
        val speed = ZOMBIE_SPEED[currentDifficulty] ?: 1000L
        val range = ZOMBIE_DETECTION_RANGE[currentDifficulty] ?: 10

        // Crear zombies
        for (i in 0 until count) {
            // Buscar posición inicial válida para los zombies (no en paredes)
            var xPos: Int
            var yPos: Int
            var attempts = 0
            do {
                xPos = Random.nextInt(10, 30)
                yPos = Random.nextInt(10, 30)
                attempts++
                // Si después de muchos intentos no encontramos una posición válida, usar una posición fija
                if (attempts > 100) {
                    Log.w(TAG, "No se pudo encontrar una posición válida después de $attempts intentos")
                    xPos = 20
                    yPos = 20
                    break
                }
            } while (!isValidPosition(xPos, yPos))

            val zombie = Zombie(
                id = "zombie_$i",
                position = Pair(xPos, yPos),
                speed = speed,
                detectionRange = range
            )

            zombies.add(zombie)

            // Notificar posición inicial del zombie
            onZombiePositionChanged(zombie.id, zombie.position)
        }
    }

    private fun startUpdateLoop() {
        // Detener el bucle existente si lo hay
        updateRunnable?.let { handler.removeCallbacks(it) }

        // Crear nuevo bucle
        val runnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    try {
                        updateZombies()
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

    private fun updateZombies() {
        zombies.forEach { it.move() }
    }

    /**
     * Establece manualmente la posición de un zombie (para sincronización)
     */
    fun setZombiePosition(zombieId: String, position: Pair<Int, Int>) {
        // Solo aceptar posiciones válidas
        if (!isValidPosition(position.first, position.second)) {
            Log.d(TAG, "Posición inválida para zombie: $position")
            return
        }

        val zombie = zombies.find { it.id == zombieId }

        if (zombie != null) {
            zombie.position = position
            zombie.lastValidPosition = position
        } else if (isGameActive) {
            // Si no existe el zombie pero el juego está activo, crear uno nuevo
            val newZombie = Zombie(
                id = zombieId,
                position = position,
                speed = ZOMBIE_SPEED[currentDifficulty]!!,
                detectionRange = ZOMBIE_DETECTION_RANGE[currentDifficulty]!!
            )
            zombies.add(newZombie)
        }

        // Verificar colisiones
        checkCollisionsWithAllPlayers()
    }

    /**
     * Revisa si algún zombie ha atrapado a algún jugador
     */
    private fun checkCollisionsWithAllPlayers() {
        for (zombie in zombies) {
            for ((playerId, playerPos) in players) {
                val distance = calculateDistance(zombie.position, playerPos)

                if (distance <= catchDistance) {
                    Log.d(TAG, "¡Zombie ${zombie.id} atrapó al jugador $playerId!")
                    onPlayerCaught()
                    return
                }
            }
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
            // Obtener la matriz de la cafetería directamente del proveedor
            val cafeteriaMatrix = MapMatrixProvider.getMatrixForMap(MapMatrixProvider.MAP_CAFETERIA)

            // Verificar colisiones con la matriz del mapa
            val cellType = cafeteriaMatrix[y][x]

            // Los zombies pueden moverse por caminos (PATH=2) y áreas interactivas (INTERACTIVE=0)
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
     * Devuelve la lista de zombies activos y sus posiciones
     */
    fun getZombies(): List<Pair<String, Pair<Int, Int>>> {
        return zombies.map { it.id to it.position }
    }

    /**
     * Devuelve el nivel de dificultad actual
     */
    fun getDifficulty(): Int {
        return currentDifficulty
    }
}