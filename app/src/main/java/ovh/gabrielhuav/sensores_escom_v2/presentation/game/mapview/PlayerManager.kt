package ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.pacman.PacmanEntityRenderer
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.pacman.PacmanController

class PlayerManager {
    private var localPlayerPosition: Pair<Int, Int>? = null
    private var remotePlayerPositions = mutableMapOf<String, PlayerInfo>()
    
    // CORRECCIÓN: Setter para limpiar el marcador temporal "player_local"
    var localPlayerId: String = "player_local"
        set(value) {
            if (field == "player_local" && value != "player_local") {
                remotePlayerPositions.remove("player_local")
            }
            field = value
        }

    private var currentMap = MapMatrixProvider.MAP_MAIN
    private val pacmanRenderer = PacmanEntityRenderer()
    private var currentPacmanDirection = PacmanController.DIRECTION_RIGHT

    data class PlayerInfo(
        val position: Pair<Int, Int>,
        val map: String
    )

    private val paintLocalPlayer = Paint().apply {
        color = Color.rgb(50, 205, 50)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }

    private val paintRemotePlayer = Paint().apply {
        color = Color.rgb(255, 105, 180)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }


    private val paintText = Paint().apply {
        color = Color.rgb(255, 255, 255)
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    interface EntityVisibilityChecker {
        fun isEntityVisible(entityId: String, position: Pair<Int, Int>): Boolean
    }

    private var visibilityChecker: EntityVisibilityChecker? = null

    fun setEntityVisibilityChecker(checker: EntityVisibilityChecker) {
        visibilityChecker = checker
    }

    fun getCurrentMap(): String = currentMap

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        localPlayerPosition = position
        position?.let {
            remotePlayerPositions[localPlayerId] = PlayerInfo(it, currentMap)
        }
    }

    fun updateRemotePlayerPositions(positions: Map<String, Pair<Int, Int>>) {
        positions.forEach { (id, pos) ->
            if (id != localPlayerId) {
                val existingPlayerInfo = remotePlayerPositions[id]
                val playerMap = existingPlayerInfo?.map ?: currentMap
                remotePlayerPositions[id] = PlayerInfo(pos, playerMap)
            }
        }
    }

    fun updateRemotePlayerPosition(playerId: String, position: Pair<Int, Int>, receivedMap: String) {
        val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)
        remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
    }

    fun drawPlayers(canvas: Canvas, mapState: MapState) {
        try {
            val bitmapWidth = mapState.backgroundBitmap?.width?.toFloat() ?: return
            val bitmapHeight = mapState.backgroundBitmap?.height?.toFloat() ?: return

            val cellWidth = bitmapWidth / MapMatrixProvider.MAP_WIDTH
            val cellHeight = bitmapHeight / MapMatrixProvider.MAP_HEIGHT

            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)

            // Filtrar jugadores y evitar duplicados huérfanos
            val playersToDraw = remotePlayerPositions.entries
                .filter {
                    val normalizedPlayerMap = MapMatrixProvider.normalizeMapName(it.value.map)
                    val isNotOrphanLocal = it.key != "player_local" || localPlayerId == "player_local"
                    normalizedPlayerMap == normalizedCurrentMap && isNotOrphanLocal
                }

            playersToDraw.forEach { (id, info) ->
                val isVisible = visibilityChecker?.isEntityVisible(id, info.position) ?: true

                if (isVisible || id == localPlayerId) {
                    val isLocal = id == localPlayerId
                    val paint = if (isLocal) paintLocalPlayer else paintRemotePlayer
                    val label = if (isLocal) "Tú" else id

                    val x = info.position.first * cellWidth + (cellWidth / 2)
                    val y = info.position.second * cellHeight + (cellHeight / 2)

                    canvas.drawCircle(x, y, cellWidth / 3, paint)
                    canvas.drawText(label, x, y - cellHeight / 2, paintText)
                }
            }
            drawSpecialEntities(canvas, cellWidth, cellHeight)
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error en drawPlayers: ${e.message}")
        }
    }

    private fun drawSpecialEntities(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        try {
            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)
            val scaleFactor = if (normalizedCurrentMap == MapMatrixProvider.MAP_SALON1212) 3.0f else 1.0f
            val adjustedCellWidth = cellWidth * scaleFactor

            specialEntities.forEach { (entityId, info) ->
                val (position, entityMap) = info
                val normalizedEntityMap = MapMatrixProvider.normalizeMapName(entityMap)

                if (normalizedEntityMap == normalizedCurrentMap) {
                    val isVisible = visibilityChecker?.isEntityVisible(entityId, position) ?: true

                    if (isVisible) {
                        val x = position.first * cellWidth + (cellWidth / 2)
                        val y = position.second * cellHeight + (cellHeight / 2)

                        when {
                            entityId == "pacman" -> {
                                pacmanRenderer.drawPacman(canvas, x, y, adjustedCellWidth, currentPacmanDirection)
                            }
                            entityId == "pacman_direction" -> {
                                try { currentPacmanDirection = entityMap.toInt() } catch(e: Exception) {}
                            }
                            entityId.startsWith("ghost_") -> {
                                pacmanRenderer.drawGhost(canvas, x, y, adjustedCellWidth, entityId)
                            }
                            entityId.startsWith("food_") -> {
                                pacmanRenderer.drawFood(canvas, x, y, adjustedCellWidth)
                            }
                            entityId == "zombie" || entityId.startsWith("zombie_") -> {
                                canvas.drawCircle(x, y, cellWidth * 0.4f, zombiePaint)
                                canvas.drawText("ZOMBIE", x, y - cellHeight * 0.7f, zombieTextPaint)
                            }
                            entityId == "rabbit" || entityId.startsWith("rabbit_") -> {
                                drawRabbit(canvas, position, cellWidth, cellHeight)
                            }
                            entityId.startsWith("item_") -> {
                                canvas.drawCircle(x, y, cellWidth * 0.3f, itemPaint)
                                canvas.drawText("ITEM", x, y - cellHeight * 0.5f, itemTextPaint)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error in drawSpecialEntities: ${e.message}")
        }
    }

    private fun drawRabbit(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        val rabbitPaint = Paint().apply {
            color = Color.rgb(255, 182, 193)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
        }
        val rabbitX = position.first * cellWidth + cellWidth / 2
        val rabbitY = position.second * cellHeight + cellHeight / 2
        canvas.drawCircle(rabbitX, rabbitY, cellWidth * 0.35f, rabbitPaint)
        canvas.drawText("🐰", rabbitX, rabbitY - cellHeight * 0.7f, paintText)
    }

    private val specialEntities = mutableMapOf<String, Pair<Pair<Int, Int>, String>>()

    fun updateSpecialEntity(entityId: String, position: Pair<Int, Int>, map: String) {
        specialEntities[entityId] = Pair(position, map)
    }

    fun removeSpecialEntity(entityId: String) {
        specialEntities.remove(entityId)
    }

    private val zombiePaint by lazy {
        Paint().apply {
            color = Color.rgb(50, 150, 50)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 3f
        }
    }

    private val zombieTextPaint by lazy {
        Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }
    }

    private val itemPaint by lazy {
        Paint().apply {
            color = Color.rgb(255, 215, 0)
            style = Paint.Style.FILL
        }
    }

    private val itemTextPaint by lazy {
        Paint().apply {
            color = Color.BLACK
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
    }

    fun setCurrentMap(map: String) {
        if (currentMap != map) {
            currentMap = map
            localPlayerPosition?.let { pos ->
                remotePlayerPositions[localPlayerId] = PlayerInfo(pos, map)
            }
        }
    }

    fun cleanup() {
        remotePlayerPositions.clear()
        localPlayerPosition = null
    }

    fun removeRemotePlayer(playerId: String) {
        remotePlayerPositions.remove(playerId)
    }

    fun getLocalPlayerPosition(): Pair<Int, Int>? = localPlayerPosition

    fun handleWebSocketMessage(message: String) {
        try {
            val jsonObject = JSONObject(message)
            when (jsonObject.getString("type")) {
                "update" -> {
                    val playerId = jsonObject.getString("id")
                    val position = Pair(jsonObject.getInt("x"), jsonObject.getInt("y"))
                    val receivedMap = jsonObject.optString("map", jsonObject.optString("currentmap", MapMatrixProvider.MAP_MAIN))
                    val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)
                    remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
                }
                "positions" -> {
                    val players = jsonObject.getJSONObject("players")
                    players.keys().forEach { playerId ->
                        if (playerId != localPlayerId) {
                            val playerData = players.getJSONObject(playerId.toString())
                            val position = Pair(playerData.getInt("x"), playerData.getInt("y"))
                            val receivedMap = playerData.optString("map", playerData.optString("currentmap", MapMatrixProvider.MAP_MAIN))
                            val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)
                            remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
                        }
                    }
                }
                "disconnect" -> {
                    val disconnectedId = jsonObject.getString("id")
                    if (disconnectedId != localPlayerId) {
                        remotePlayerPositions.remove(disconnectedId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error processing WebSocket message", e)
        }
    }

    fun getSpecialEntitiesCount(): Int = specialEntities.size

    fun logSpecialEntities() {
        specialEntities.forEach { (id, info) ->
            Log.d("PlayerManager", "Entidad especial: $id en posición ${info.first}, mapa ${info.second}")
        }
    }
}
