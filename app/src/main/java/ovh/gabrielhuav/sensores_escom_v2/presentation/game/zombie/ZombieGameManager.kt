package ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie

import android.os.Handler
import android.os.Looper
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrix
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Gestor global del minijuego de zombies.
 * - Un único loop controlado por Handler.
 * - Límite de zombies por mapa según dificultad.
 * - Spawn "seguro" (lejos del jugador) para evitar instakill.
 * - Limpieza robusta al detener el juego, notificando a listeners.
 */
object ZombieGameManager {

    // Dificultades públicas
    const val DIFFICULTY_EASY = 1
    const val DIFFICULTY_MEDIUM = 2
    const val DIFFICULTY_HARD = 3

    interface Listener {
        /** Reporte de posición de O CADA zombie que exista. */
        fun onZombiePosition(zombieId: String, mapId: String, position: Pair<Int, Int>)

        /** Un zombie atrapó a un jugador. */
        fun onPlayerCaught(victimId: String)

        /** El juego se detuvo (por captura o manualmente). Úsalo para limpiar sprites/UI. */
        fun onGameStopped(reason: String) {}
    }

    // ================== Estado global ==================
    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null
    private var isRunning = false

    private val listeners = mutableSetOf<Listener>()

    /** playersById[id] = (mapId, position) */
    private val playersById = mutableMapOf<String, Pair<String, Pair<Int, Int>>>()

    /** zombiesByMap[mapId][zombieId] = position */
    private val zombiesByMap = mutableMapOf<String, MutableMap<String, Pair<Int, Int>>>()

    // Parámetros de dificultad (más "light")
    private var maxZombiesPerMap = 2
    private var tickMs = 950L
    private var baseTickMs = tickMs

    // Spawn seguro
    private const val MIN_SPAWN_DIST = 4
    private const val MAX_SPAWNS_PER_TICK = 1

    // Ralentización temporal
    private var slowDownRestoreRunnable: Runnable? = null

    // ================== API pública ==================

    fun addListener(l: Listener) { listeners += l }
    fun removeListener(l: Listener) { listeners -= l }

    @Synchronized
    fun startGame(difficulty: Int) {
        applyDifficulty(difficulty)

        if (isRunning) {
            Log.d(TAG, "ZombieGameManager already running. New difficulty applied.")
            return
        }

        isRunning = true
        // no limpiamos players (cada Activity se registra/actualiza)
        zombiesByMap.values.forEach { it.clear() }

        ticker?.let { handler.removeCallbacks(it) }
        ticker = object : Runnable {
            override fun run() {
                if (!isRunning) return
                try {
                    tick()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in zombie tick: ${e.message}", e)
                }
                if (isRunning) handler.postDelayed(this, tickMs)
            }
        }
        handler.post(ticker!!)
        Log.d(TAG, "ZombieGameManager started: diff=$difficulty tick=$tickMs maxZ=$maxZombiesPerMap")
    }

    @Synchronized
    fun stopGame(reason: String = "stopped") {
        if (!isRunning) return
        isRunning = false

        ticker?.let { handler.removeCallbacks(it) }
        slowDownRestoreRunnable?.let { handler.removeCallbacks(it) }
        ticker = null
        slowDownRestoreRunnable = null

        // Limpiar TODAS las hordas
        zombiesByMap.values.forEach { it.clear() }

        // Avisar a las UIs para que borren sprites/HUD
        listeners.forEach { it.onGameStopped(reason) }
        Log.d(TAG, "ZombieGameManager stopped. reason=$reason")
    }

    /** Registrar/actualizar a un jugador (id, mapId normalizado, posición) */
    fun updatePlayer(playerId: String, mapId: String, position: Pair<Int, Int>) {
        val normalized = MapMatrixProvider.normalizeMapName(mapId)
        playersById[playerId] = normalized to position
    }

    /**
     * Ralentiza zombies: aumenta el periodo de tick temporalmente.
     */
    fun slowDownZombies(durationMs: Long) {
        if (!isRunning) return
        val currentBase = baseTickMs
        baseTickMs = max(currentBase, tickMs) // conservar base válida
        tickMs = (tickMs * 1.6f).toLong().coerceAtMost(1500L)

        slowDownRestoreRunnable?.let { handler.removeCallbacks(it) }
        slowDownRestoreRunnable = Runnable {
            tickMs = baseTickMs
            Log.d(TAG, "Zombie speed restored. tick=$tickMs")
        }
        handler.postDelayed(slowDownRestoreRunnable!!, durationMs)
        Log.d(TAG, "Zombies slowed for ${durationMs}ms. tick=$tickMs")
    }

    /**
     * Traslada la horda actual a otro mapa (p.ej. cuando sales/entras de Cafetería).
     * Si se da anchor: reubica cada zombie cerca del anchor con un pequeño jitter seguro.
     */
    fun transferHordeToMap(targetMap: String, anchor: Pair<Int, Int>? = null) {
        val normalized = MapMatrixProvider.normalizeMapName(targetMap)

        val zMap = zombiesByMap.getOrPut(normalized) { mutableMapOf() }
        // Reposicionar todos los zombies ya existentes (de todos los mapas) en el nuevo mapa
        val all = zombiesByMap.values.flatMap { it.entries }.toMutableList()
        zombiesByMap.values.forEach { it.clear() }

        val width = MapMatrixProvider.MAP_WIDTH
        val height = MapMatrixProvider.MAP_HEIGHT

        all.forEach { (zId, _) ->
            val pos = if (anchor != null) {
                val ax = anchor.first
                val ay = anchor.second
                val jx = (-1..1).random()
                val jy = (-1..1).random()
                Pair(
                    (ax + jx).coerceIn(1, width - 2),
                    (ay + jy).coerceIn(1, height - 2)
                )
            } else {
                // sin anchor: posición segura random cerca del (primer) jugador del mapa si existe
                val playersHere = playersById.filterValues { it.first == normalized }.values.map { it.second }
                if (playersHere.isNotEmpty()) safeSpawnNear(normalized, playersHere)
                else Pair(width / 2, height / 2)
            }

            zMap[zId] = pos
        }

        // Notificar posiciones en el nuevo mapa
        zMap.forEach { (id, p) -> listeners.forEach { it.onZombiePosition(id, normalized, p) } }
    }

    /** Elimina a un jugador del seguimiento (por desconexión/salida) */
    fun removePlayer(playerId: String) {
        playersById.remove(playerId)
    }

    // ================== Lógica interna ==================

    private fun applyDifficulty(difficulty: Int) {
        when (difficulty) {
            DIFFICULTY_HARD   -> { maxZombiesPerMap = 4; tickMs = 650L }
            DIFFICULTY_MEDIUM -> { maxZombiesPerMap = 3; tickMs = 800L }
            else              -> { maxZombiesPerMap = 2; tickMs = 950L } // EASY
        }
        baseTickMs = tickMs
    }

    private fun tick() {
        // Agrupar jugadores por mapa
        val playersByMap: Map<String, List<Pair<String, Pair<Int, Int>>>> =
            playersById.entries.groupBy(
                keySelector = { it.value.first },                 // mapId
                valueTransform = { it.key to it.value.second }    // (playerId to position)
            )

        playersByMap.forEach { (mapId, playersInMap) ->
            if (playersInMap.isEmpty()) return@forEach

            val normalizedMap = MapMatrixProvider.normalizeMapName(mapId)
            val zMap = zombiesByMap.getOrPut(normalizedMap) { mutableMapOf() }

            // ---- Spawn suave y seguro ----
            var spawnsThisTick = 0
            while (zMap.size < maxZombiesPerMap && spawnsThisTick < MAX_SPAWNS_PER_TICK) {
                val spawn = safeSpawnNear(normalizedMap, playersInMap.map { it.second })
                val newId = "zombie_${System.nanoTime()}"
                zMap[newId] = spawn
                listeners.forEach { it.onZombiePosition(newId, normalizedMap, spawn) }
                spawnsThisTick++
            }

            // ---- Movimiento ----
            val updated = mutableMapOf<String, Pair<Int, Int>>()
            for ((zId, zPos) in zMap) {
                val target = playersInMap.minByOrNull { manhattan(zPos, it.second) } ?: continue

                // Si ya está pegado, cuenta como captura y detiene
                if (manhattan(zPos, target.second) <= 1) {
                    listeners.forEach { it.onPlayerCaught(target.first) }
                    stopGame(reason = "caught:${target.first}")
                    return
                }

                val next = stepTowards(zPos, target.second)
                updated[zId] = next
                listeners.forEach { it.onZombiePosition(zId, normalizedMap, next) }
            }
            zMap.putAll(updated)
        }
    }

    private fun manhattan(a: Pair<Int, Int>, b: Pair<Int, Int>): Int =
        abs(a.first - b.first) + abs(a.second - b.second)

    private fun stepTowards(from: Pair<Int, Int>, to: Pair<Int, Int>): Pair<Int, Int> {
        val dx = to.first - from.first
        val dy = to.second - from.second
        val nx = from.first + dx.coerceIn(-1, 1)
        val ny = from.second + dy.coerceIn(-1, 1)
        return nx to ny
    }

    /** Spawn cerca pero a distancia mínima del jugador más cercano; valida contra matriz si existe. */
    private fun safeSpawnNear(mapId: String, playerPositions: List<Pair<Int, Int>>): Pair<Int, Int> {
        val p = playerPositions.random()
        val width = MapMatrixProvider.MAP_WIDTH
        val height = MapMatrixProvider.MAP_HEIGHT
        val matrix = try { MapMatrix(mapId) } catch (_: Exception) { null }

        repeat(30) {
            val rx = clamp(p.first + (-7..7).random(), 1, width - 2)
            val ry = clamp(p.second + (-7..7).random(), 1, height - 2)
            val candidate = rx to ry

            if (manhattan(candidate, p) < MIN_SPAWN_DIST) return@repeat
            if (matrix == null || matrix.isValidPosition(rx, ry)) return candidate
        }

        // Fallback: a 4-5 celdas en línea recta
        val fx = clamp(p.first + listOf(-5, -4, 4, 5).random(), 1, width - 2)
        val fy = clamp(p.second + listOf(-5, -4, 4, 5).random(), 1, height - 2)
        return fx to fy
    }

    private fun clamp(v: Int, lo: Int, hi: Int) = min(max(v, lo), hi)

    private const val TAG = "ZombieGameManager"
}
