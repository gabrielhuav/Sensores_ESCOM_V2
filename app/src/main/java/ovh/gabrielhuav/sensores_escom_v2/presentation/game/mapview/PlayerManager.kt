package ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.pacman.PacmanEntityRenderer
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.pacman.PacmanController

class PlayerManager(private val context: Context) {

    private var localPlayerPosition: Pair<Int, Int>? = null
    private var remotePlayerPositions = mutableMapOf<String, PlayerInfo>()
    var localPlayerId: String = "player_local"
    private var currentMap = MapMatrixProvider.MAP_MAIN
    private val pacmanRenderer = PacmanEntityRenderer()
    private var currentPacmanDirection = PacmanController.DIRECTION_RIGHT

    // Mapa para entidades especiales (esimios, zombies, etc)
    private val specialEntities = mutableMapOf<String, Pair<Pair<Int, Int>, String>>()

    // Cache para drawables de esimios
    private var esimioDrawable: Drawable? = null

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

    // Paints para esimios
    private val esimioPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val esimioBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val esimioTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }

    // Inicializar drawable de esimio
    init {
        try {
            esimioDrawable = context.resources.getDrawable(R.drawable.esimio, null)
            Log.d("PlayerManager", "Drawable esimio cargado exitosamente")
        } catch (e: Exception) {
            Log.w("PlayerManager", "No se pudo cargar esimio.png, usando fallback: ${e.message}")
        }
    }

    interface EntityVisibilityChecker {
        fun isEntityVisible(entityId: String, position: Pair<Int, Int>): Boolean
    }

    private var visibilityChecker: EntityVisibilityChecker? = null

    fun setEntityVisibilityChecker(checker: EntityVisibilityChecker) {
        visibilityChecker = checker
    }

    fun getCurrentMap(): String = currentMap

    // Método para actualizar entidades especiales
    fun updateSpecialEntity(entityId: String, position: Pair<Int, Int>, map: String) {
        specialEntities[entityId] = Pair(position, map)
        Log.d("PlayerManager", "Entidad especial actualizada: $entityId en $position, mapa: $map")
    }

    // Método para remover entidades especiales
    fun removeSpecialEntity(entityId: String) {
        specialEntities.remove(entityId)
        Log.d("PlayerManager", "Entidad especial removida: $entityId")
    }

    // Contar entidades especiales
    fun getSpecialEntitiesCount(): Int = specialEntities.size

    // Log de entidades especiales
    fun logSpecialEntities() {
        specialEntities.forEach { (id, info) ->
            Log.d("PlayerManager", "  $id -> pos=${info.first}, map=${info.second}")
        }
    }

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        Log.d("PlayerManager", "Updating local player position: $position in map: $currentMap")
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
        Log.d("PlayerManager", "Updated player $playerId position: $position in normalized map: $normalizedMap")
    }

    private fun normalizeMapId(mapId: String): String {
        return when {
            mapId.contains("cafeteria") -> MapMatrixProvider.MAP_CAFETERIA
            mapId.contains("salon2009") -> MapMatrixProvider.MAP_SALON2009
            mapId.contains("salon2010") -> MapMatrixProvider.MAP_SALON2010
            mapId.contains("building2") -> MapMatrixProvider.MAP_BUILDING2
            mapId.contains("main") -> MapMatrixProvider.MAP_MAIN
            mapId.contains("escom_building2_piso1") -> MapMatrixProvider.MAP_BUILDING2_PISO1
            else -> mapId
        }
    }

    private fun drawItem(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        val itemPaint = Paint().apply {
            color = Color.rgb(255, 215, 0)
            style = Paint.Style.FILL
        }

        val itemTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        val itemX = position.first * cellWidth + cellWidth / 2
        val itemY = position.second * cellHeight + cellHeight / 2

        canvas.drawCircle(itemX, itemY, cellWidth * 0.3f, itemPaint)
        canvas.drawText("ITEM", itemX, itemY - cellHeight * 0.5f, itemTextPaint)
    }

    private fun drawRabbit(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        val rabbitPaint = Paint().apply {
            color = Color.rgb(255, 182, 193)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
        }

        val rabbitTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }

        val rabbitX = position.first * cellWidth + cellWidth / 2
        val rabbitY = position.second * cellHeight + cellHeight / 2

        canvas.drawCircle(rabbitX, rabbitY, cellWidth * 0.35f, rabbitPaint)
        canvas.drawText("🐰", rabbitX, rabbitY - cellHeight * 0.7f, rabbitTextPaint)
    }

    private fun drawZombie(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        val zombiePaint = Paint().apply {
            color = Color.rgb(50, 150, 50)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 3f
        }

        val zombieTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }

        val zombieX = position.first * cellWidth + cellWidth / 2
        val zombieY = position.second * cellHeight + cellHeight / 2

        canvas.drawCircle(zombieX, zombieY, cellWidth * 0.4f, zombiePaint)
        canvas.drawText("ZOMBIE", zombieX, zombieY - cellHeight * 0.7f, zombieTextPaint)
    }

    // Método para dibujar esimios
    private fun drawEsimio(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        val esimioX = position.first * cellWidth + cellWidth / 2
        val esimioY = position.second * cellHeight + cellHeight / 2

        // Intentar usar el drawable primero
        esimioDrawable?.let { drawable ->
            try {
                val drawableSize = cellWidth * 1.2f
                drawable.setBounds(
                    (esimioX - drawableSize / 2).toInt(),
                    (esimioY - drawableSize / 2).toInt(),
                    (esimioX + drawableSize / 2).toInt(),
                    (esimioY + drawableSize / 2).toInt()
                )
                drawable.draw(canvas)
                Log.d("PlayerManager", "Esimio dibujado con PNG en ($esimioX, $esimioY)")
                return
            } catch (e: Exception) {
                Log.w("PlayerManager", "Error dibujando esimio con PNG: ${e.message}")
            }
        }

        // Fallback: dibujar círculo rojo con "E"
        canvas.drawCircle(esimioX, esimioY, cellWidth * 0.4f, esimioPaint)
        canvas.drawCircle(esimioX, esimioY, cellWidth * 0.4f, esimioBorderPaint)
        canvas.drawText("E", esimioX, esimioY + cellHeight * 0.1f, esimioTextPaint)

        Log.d("PlayerManager", "Esimio dibujado con fallback en ($esimioX, $esimioY)")
    }

    // NUEVO: Método para dibujar entidades genéricas como fallback
    private fun drawGenericEntity(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float, entityId: String) {
        val entityX = position.first * cellWidth + cellWidth / 2
        val entityY = position.second * cellHeight + cellHeight / 2

        val paint = Paint().apply {
            color = when (entityId.hashCode() % 6) {
                0 -> Color.RED
                1 -> Color.BLUE
                2 -> Color.GREEN
                3 -> Color.MAGENTA
                4 -> Color.CYAN
                else -> Color.YELLOW
            }
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = cellWidth * 0.3f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 0f, 0f, Color.BLACK)
        }

        canvas.drawCircle(entityX, entityY, cellWidth * 0.3f, paint)
        canvas.drawCircle(entityX, entityY, cellWidth * 0.3f, borderPaint)

        // Mostrar las primeras 3 letras del ID
        val displayText = if (entityId.length > 3) entityId.substring(0, 3) else entityId
        canvas.drawText(displayText.uppercase(), entityX, entityY + cellHeight * 0.1f, textPaint)

        Log.d("PlayerManager", "Entidad genérica dibujada: $entityId en ($entityX, $entityY)")
    }

    fun drawPlayers(canvas: Canvas, mapState: MapState) {
        try {
            val bitmapWidth = mapState.backgroundBitmap?.width?.toFloat() ?: return
            val bitmapHeight = mapState.backgroundBitmap?.height?.toFloat() ?: return

            val cellWidth = bitmapWidth / MapMatrixProvider.MAP_WIDTH
            val cellHeight = bitmapHeight / MapMatrixProvider.MAP_HEIGHT

            Log.d("PlayerManager", "Dibujando jugadores en mapa: $currentMap")
            Log.d("PlayerManager", "Total jugadores: ${remotePlayerPositions.size}")

            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)

            val playersToDraw = remotePlayerPositions.entries
                .filter {
                    val normalizedPlayerMap = MapMatrixProvider.normalizeMapName(it.value.map)
                    normalizedPlayerMap == normalizedCurrentMap
                }

            Log.d("PlayerManager", "Jugadores a dibujar: ${playersToDraw.size} en mapa $normalizedCurrentMap")

            // Dibujar cada jugador
            playersToDraw.forEach { (id, info) ->
                val isVisible = visibilityChecker?.isEntityVisible(id, info.position) ?: true

                if (isVisible || id == localPlayerId) {
                    val paint = if (id == localPlayerId) paintLocalPlayer else paintRemotePlayer
                    val label = if (id == localPlayerId) "Tú" else id

                    val x = info.position.first * cellWidth + (cellWidth / 2)
                    val y = info.position.second * cellHeight + (cellHeight / 2)

                    canvas.drawCircle(x, y, cellWidth / 3, paint)
                    canvas.drawText(label, x, y - cellHeight / 2, paintText)

                    Log.d("PlayerManager", "Dibujado jugador $id en ($x, $y)")
                }
            }

            // Dibujar entidades especiales (incluyendo esimios)
            drawSpecialEntities(canvas, cellWidth, cellHeight)
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error en drawPlayers: ${e.message}")
        }
    }

    // MODIFICADO: Método mejorado para dibujar entidades especiales
    private fun drawSpecialEntities(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        try {
            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)

            val scaleFactor = if (normalizedCurrentMap == MapMatrixProvider.MAP_SALON1212) 3.0f else 1.0f
            val adjustedCellWidth = cellWidth * scaleFactor
            val adjustedCellHeight = cellHeight * scaleFactor

            Log.d("PlayerManager", "Drawing special entities with cells: $adjustedCellWidth x $adjustedCellHeight")

            specialEntities.forEach { (entityId, info) ->
                val (position, entityMap) = info
                val normalizedEntityMap = MapMatrixProvider.normalizeMapName(entityMap)

                if (normalizedEntityMap == normalizedCurrentMap) {
                    val isVisible = visibilityChecker?.isEntityVisible(entityId, position) ?: true

                    if (isVisible) {
                        val x = position.first * cellWidth + (cellWidth / 2)
                        val y = position.second * cellHeight + (cellHeight / 2)

                        when {
                            // Caso para esimios
                            entityId.startsWith("esimio_") -> {
                                drawEsimio(canvas, position, cellWidth, cellHeight)
                                Log.d("PlayerManager", "Esimio $entityId dibujado en ($x, $y)")
                            }
                            entityId == "pacman" -> {
                                Log.d("PlayerManager", "Drawing Pacman at ($x, $y)")
                                try {
                                    pacmanRenderer.drawPacman(canvas, x, y, adjustedCellWidth, currentPacmanDirection)
                                } catch(e: Exception) {
                                    // Fallback para Pacman
                                    val pacmanPaint = Paint().apply {
                                        color = Color.YELLOW
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawCircle(x, y, adjustedCellWidth * 0.7f, pacmanPaint)
                                }
                            }
                            entityId == "pacman_direction" -> {
                                try {
                                    currentPacmanDirection = entityMap.toInt()
                                    Log.d("PlayerManager", "Actualizada dirección de Pacman: $currentPacmanDirection")
                                } catch(e: Exception) {
                                    Log.e("PlayerManager", "Error actualizando dirección de Pacman: ${e.message}")
                                }
                            }
                            entityId.startsWith("ghost_") -> {
                                try {
                                    pacmanRenderer.drawGhost(canvas, x, y, adjustedCellWidth, entityId)
                                } catch(e: Exception) {
                                    // Fallback manual para fantasmas
                                    val ghostPaint = Paint().apply {
                                        color = when {
                                            entityId.endsWith("0") -> Color.RED
                                            entityId.endsWith("1") -> Color.rgb(255, 184, 255)
                                            entityId.endsWith("2") -> Color.CYAN
                                            else -> Color.rgb(255, 184, 82)
                                        }
                                        style = Paint.Style.FILL
                                        setShadowLayer(5f, 0f, 0f, Color.GRAY)
                                    }
                                    canvas.drawCircle(x, y, adjustedCellWidth * 0.7f, ghostPaint)
                                }
                            }
                            entityId.startsWith("food_") -> {
                                try {
                                    pacmanRenderer.drawFood(canvas, x, y, adjustedCellWidth)
                                } catch(e: Exception) {
                                    val foodPaint = Paint().apply {
                                        color = Color.rgb(255, 223, 0)
                                        style = Paint.Style.FILL
                                        setShadowLayer(5f, 0f, 0f, Color.WHITE)
                                    }
                                    canvas.drawCircle(x, y, adjustedCellWidth * 0.3f, foodPaint)
                                }
                            }
                            entityId == "zombie" || entityId.startsWith("zombie_") -> {
                                drawZombie(canvas, position, cellWidth, cellHeight)
                            }
                            entityId == "rabbit" || entityId.startsWith("rabbit_") -> {
                                drawRabbit(canvas, position, cellWidth, cellHeight)
                            }
                            entityId.startsWith("item_") -> {
                                drawItem(canvas, position, cellWidth, cellHeight)
                            }
                            // NUEVO: Fallback para entidades desconocidas
                            else -> {
                                drawGenericEntity(canvas, position, cellWidth, cellHeight, entityId)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error in drawSpecialEntities: ${e.message}")
            e.printStackTrace()
        }
    }

    // Paints existentes para otras entidades
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

    private fun drawPlayer(canvas: Canvas, position: Pair<Int, Int>, playerId: String, paint: Paint, cellWidth: Float, cellHeight: Float) {
        val playerX = position.first * cellWidth + cellWidth / 2
        val playerY = position.second * cellHeight + cellHeight / 2

        canvas.drawCircle(playerX, playerY, cellWidth / 3f, paint)
        canvas.drawText(playerId, playerX, playerY - cellHeight / 2, paintText)

        Log.d("PlayerManager", "Jugador $playerId dibujado en posición ($playerX, $playerY)")
    }

    fun setCurrentMap(map: String) {
        if (currentMap != map) {
            Log.d("PlayerManager", "⚠️ Current map changed from $currentMap to $map")
            currentMap = map
            localPlayerPosition?.let { pos ->
                remotePlayerPositions[localPlayerId] = PlayerInfo(pos, map)
                Log.d("PlayerManager", "Actualizando posición del jugador local en nuevo mapa: $pos")
            }
        }
    }

    fun cleanup() {
        remotePlayerPositions.clear()
        localPlayerPosition = null
        specialEntities.clear() // Limpiar entidades especiales también
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
                    val position = Pair(
                        jsonObject.getInt("x"),
                        jsonObject.getInt("y")
                    )

                    val receivedMap = if (jsonObject.has("map")) {
                        jsonObject.getString("map")
                    } else if (jsonObject.has("currentmap")) {
                        jsonObject.getString("currentmap")
                    } else {
                        MapMatrixProvider.MAP_MAIN
                    }

                    val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)
                    remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
                    Log.d("PlayerManager", "Updated from update: player=$playerId, pos=$position, map=$normalizedMap")
                }
                "positions" -> {
                    val players = jsonObject.getJSONObject("players")
                    players.keys().forEach { playerId ->
                        if (playerId != localPlayerId) {
                            val playerData = players.getJSONObject(playerId.toString())
                            val position = Pair(
                                playerData.getInt("x"),
                                playerData.getInt("y")
                            )

                            val receivedMap = if (playerData.has("map")) {
                                playerData.getString("map")
                            } else if (playerData.has("currentmap")) {
                                playerData.getString("currentmap")
                            } else {
                                playerData.optString("currentMap", MapMatrixProvider.MAP_MAIN)
                            }

                            val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)
                            remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
                            Log.d("PlayerManager", "Updated from positions: player=$playerId, map=$normalizedMap")
                        }
                    }
                }
                "disconnect" -> {
                    val disconnectedId = jsonObject.getString("id")
                    if (disconnectedId != localPlayerId) {
                        remotePlayerPositions.remove(disconnectedId)
                        Log.d("PlayerManager", "Removed disconnected player: $disconnectedId")
                    }
                }
                // Manejar mensajes de esimios
                "esimio_position" -> {
                    val esimioId = jsonObject.optString("id", "esimio_0")
                    val x = jsonObject.getInt("x")
                    val y = jsonObject.getInt("y")
                    val map = jsonObject.optString("map", MapMatrixProvider.MAP_ESIME)

                    updateSpecialEntity(esimioId, Pair(x, y), map)
                    Log.d("PlayerManager", "Esimio position updated: $esimioId at ($x, $y) in $map")
                }
                "esimio_game_command" -> {
                    val command = jsonObject.optString("command")
                    when (command) {
                        "start", "stop" -> {
                            // Limpiar esimios cuando el juego empieza o termina
                            specialEntities.keys.filter { it.startsWith("esimio_") }.forEach {
                                removeSpecialEntity(it)
                            }
                            Log.d("PlayerManager", "Esimio game $command - cleared esimio entities")
                        }
                    }
                }
                // NUEVO: Manejar mensajes genéricos de entidades especiales
                "special_entity_update" -> {
                    val entityId = jsonObject.getString("entityId")
                    val x = jsonObject.getInt("x")
                    val y = jsonObject.getInt("y")
                    val map = jsonObject.optString("map", currentMap)

                    updateSpecialEntity(entityId, Pair(x, y), map)
                    Log.d("PlayerManager", "Entidad especial actualizada via WebSocket: $entityId at ($x, $y) in $map")
                }
                "special_entity_remove" -> {
                    val entityId = jsonObject.getString("entityId")
                    removeSpecialEntity(entityId)
                    Log.d("PlayerManager", "Entidad especial removida via WebSocket: $entityId")
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error processing WebSocket message", e)
        }
    }
}