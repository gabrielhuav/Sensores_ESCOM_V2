package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.locations.esime.buildings



import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.Esime
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings.MinijuegoEnergiaActivity

class Edificio3Activity : AppCompatActivity(), OnlineServerManager.WebSocketListener, MapView.MapTransitionListener {

    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

    // 🎒 "Inventario" sencillo para los puzzles
    private var tieneFusible = false
    private var tieneContrasena = false
    private var energiaRestaurada = false
    private lateinit var btnNorth: MaterialButton
    private lateinit var btnSouth: MaterialButton
    private lateinit var btnEast: MaterialButton
    private lateinit var btnWest: MaterialButton
    private lateinit var buttonA: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var playerName: String
    private var isServer: Boolean = false
    private var canExitBuilding = false

    // 🏗️ SISTEMA DE PISOS MÚLTIPLES
    private var currentFloor: Int = 0 // 0 = Planta baja, 1 = Piso 1, 2 = Piso 2, 3 = Piso 3
    private val maxFloors = 3 // Máximo piso (0-3)
    
    // Mapas de recursos para cada piso
    private val floorMaps = mapOf(
        0 to R.drawable.mapa_edificio3_esime,     // Planta baja
        1 to R.drawable.mapa_edificio3_esime_1,   // Piso 1
        2 to R.drawable.mapa_edificio3_esime_2,   // Piso 2
        3 to R.drawable.mapa_edificio3_esime_3    // Piso 3
    )

    // Posiciones de las escaleras/elevador (donde se puede subir/bajar)
    // 📍 Ubicación: X=34, Y=21
    private val stairsPositions = listOf(
        Pair(34, 21)  // Posición exacta de las escaleras
    )
    private val stairsCenterPosition = Pair(34, 21)

    // 🟢 POSICIÓN INICIAL SEGURA EN EL PASILLO
    private var playerPosition: Pair<Int, Int> = Pair(3, 19)

    private val collisionMap: Array<BooleanArray> by lazy { createCollisionMap() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esime)

        try {
            initializeViews()

            mapView = MapView(context = this, mapResourceId = R.drawable.mapa_edificio3_esime)
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"
            isServer = intent.getBooleanExtra("IS_SERVER", false)

            mapView.post {
                // Inicializar con el mapa de la planta baja
                updateFloorMap(currentFloor)
                mapView.playerManager.apply {
                    setCurrentMap("edificio3_piso_$currentFloor")
                    localPlayerId = playerName
                    updateLocalPlayerPosition(playerPosition)
                }
                initializeManagers()
                setupButtonListeners()
                setupStairsVisual()  // 📍 Agregar visual de escaleras
                updateFloorDisplay()
            }
        } catch (e: Exception) {
            Log.e("Edificio3", "Error: ${e.message}")
            finish()
        }
    }

    private fun createCollisionMap(): Array<BooleanArray> {
        val cols = 40
        val rows = 40
        val collisions = Array(cols) { BooleanArray(rows) { false } }

        // 1. LIMITES EXTERNOS (Para que no salgas del mapa negro)
        for (x in 0 until cols) {
            collisions[x][0] = true; collisions[x][39] = true
        }
        for (y in 0 until rows) {
            collisions[0][y] = true; collisions[39][y] = true
        }

        // 2. MUROS HORIZONTALES LARGOS
        for (x in 3..37) {
            collisions[x][7] = true  // Techo de aulas superiores
            collisions[x][18] = true // Muro superior del pasillo
            collisions[x][22] = true // Muro inferior del pasillo
            collisions[x][32] = true // Suelo de aulas inferiores
        }

        // 3. MUROS VERTICALES (Divisores de Aulas)
        // Inferiores
        for (y in 23..32) {
            collisions[8][y] = true  // Izquierda Aula 7
            collisions[15][y] = true // Entre Aula 7 y 8
            collisions[22][y] = true // Entre Aula 8 y 9
            collisions[30][y] = true // Derecha Aula 9
        }

        // Superiores
        for (y in 7..18) {
            collisions[7][y] = true  // Izquierda Aula 2 (Pared doble)
            collisions[8][y] = true  // Izquierda Aula 2 (Pared doble)
            collisions[13][y] = true // Entre Aula 2 y 3

            collisions[19][y] = true // Entre Aula 3 y 4

            collisions[25][y] = true // Entre Aula 4 y 5
        }
        for (y in 7..17) {
            collisions[31][y] = true // Derecha Aula 5
        }

        // 4. ABRIR EL VESTÍBULO (Pasillo vertical a la izquierda)
        for (x in 3..7) {
            collisions[x][17] = false
            collisions[x][18] = false // Romper el muro superior del pasillo aquí
            collisions[x][22] = false // Romper el muro inferior del pasillo aquí
        }

        // 5. PUERTAS EXACTAS (Huecos en los muros)
        // Puertas Superiores (Y = 18)
        collisions[8][18] = false; collisions[9][18] = false   // Aula 2
        collisions[14][18] = false; collisions[15][18] = false // Aula 3
        collisions[20][18] = false                             // Aula 4
        collisions[26][18] = false                             // Aula 5

        // Puertas Inferiores (Y = 22)
        collisions[10][22] = false                             // Aula 7
        collisions[17][22] = false; collisions[18][22] = false // Aula 8
        collisions[24][22] = false; collisions[25][22] = false // Aula 9

        return collisions
    }

    private fun getRoomName(position: Pair<Int, Int>): String {
        val (x, y) = position

        // 🟢 DETECTAR SI ESTAMOS EN LAS ESCALERAS
        if (isAtStairs(position)) {
            return "Escaleras"
        }

        // 1. ZONAS DE PASILLO
        if (y in 19..21) return "Pasillo Principal"
        if (y == 18 || y == 22) return "En la Puerta"
        if (x in 3..7 && (y in 8..17 || y in 23..31)) return "Vestíbulo (Pasillo Vertical)"

        // 2. ZONA SUPERIOR (Arriba del pasillo, Y entre 8 y 17)
        if (y in 8..17) {
            return when {
                x in 9..12 -> "Aula 2"
                x in 14..24 -> "Aula 3"
                x in 26..30 -> "Aula 4"
                x > 31 -> "Aula 5"
                else -> "Muro"
            }
        }

        // 3. ZONA INFERIOR (Abajo del pasillo, Y entre 23 y 31)
        if (y in 23..31) {
            return when {
                x in 9..14 -> "Aula 7"
                x in 16..21 -> "Aula 8"
                x in 23..29 -> "Aula 9"
                x > 30 -> "Escaleras / Mantenimiento"
                else -> "Muro"
            }
        }

        return "Desconocido (X=$x, Y=$y)"
    }

    // 🟢 VERIFICAR SI ESTAMOS EN LAS ESCALERAS
    private fun isAtStairs(position: Pair<Int, Int>): Boolean {
        return stairsPositions.contains(position)
    }

    // 🟢 VALIDACIÓN DE COLISIONES ACTIVA
    private fun isValidPosition(position: Pair<Int, Int>): Boolean {
        val (x, y) = position
        if (x < 0 || x >= 40 || y < 0 || y >= 40) return false
        
        // Validar colisiones estáticas del mapa
        if (collisionMap[x][y]) return false

        // 🏗️ VALIDACIONES DINÁMICAS POR PISO
        // En pisos superiores (1, 2, 3), ajustes de colisiones específicas
        if (currentFloor != 0) {
            // Bloquear salida derecha: x >= 36 cuando y = 18, 19, 20
            if (x >= 36 && y in 18..21) {
                return false
            }
            
            // Bloquear salida izquierda: x <= 6 cuando y = 18, 19, 20
            if (x <= 6 && y in 17..30) {
                return false
            }
            
            // 🚪 PERMITIR PUERTAS QUE AHORA SON ENTRADAS (Excepciones)
            // Estas posiciones NO deben tener colisión en pisos superiores
            val entrancesAllowed = listOf(
                Pair(31, 18),  // Baños/derecha
                Pair(21, 18),  // Aula 3/entrada
                Pair(10, 18),  // Aula 2/entrada
                Pair(12, 18),  // Aula 2/entrada
                Pair(19, 21),  // Aula 3/pasillo
                Pair(13, 21)   // Aula 2/pasillo
            )
            if (entrancesAllowed.contains(position)) {
                return true  // Permitir pasar
            }
            
            // 🚫 BLOQUEAR POSICIONES QUE NO DEBEN TENER PASO
            val blockedPositions = listOf(
                Pair(9, 20),   // Quitar colisión indebida
                Pair(20, 18),  // Nueva colisión (Aula 3/4)
                Pair(15, 18),  // Nueva colisión (entre aulas)
                Pair(14, 18),  // Nueva colisión (Aula 3/entrada)
                Pair(18, 22),  // Nueva colisión (pasillo inferior)
                Pair(17, 22)   // Nueva colisión (Aula 8)
            )
            if (blockedPositions.contains(position)) {
                return false  // Bloquear estas posiciones
            }
        }
        
        return true
    }

    private fun initializeManagers() {
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@Edificio3Activity)
        }
        serverConnectionManager = ServerConnectionManager(this, onlineServerManager)

        movementManager = MovementManager(mapView) { newPosition ->
            if (isValidPosition(newPosition)) {
                playerPosition = newPosition
                updatePlayerPosition(newPosition)
            } else {
                movementManager.setPosition(playerPosition) // Previene que el personaje atraviese paredes
            }
        }

        mapView.setMapTransitionListener(this)
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(playerPosition)
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        buttonA.setOnClickListener {
            if (canExitBuilding) {
                returnToEsime()
            } else {
                val room = getRoomName(playerPosition)

                when (room) {
                    "Escaleras" -> {
                        // 🟢 LÓGICA DE ESCALERAS - Permitir subir y bajar
                        showStairsMenu()
                    }
                    "Aula 2" -> {
                        if (energiaRestaurada) {
                            Toast.makeText(this, "⚡ La energía ya está funcionando perfectamente.", Toast.LENGTH_SHORT).show()
                        } else if (tieneFusible) {
                            // Si ya trajo el fusible, AHORA SÍ abrimos el minijuego
                            val intent = Intent(this, MinijuegoEnergiaActivity::class.java)
                            // Le pasamos el dato de si conoce la contraseña o no
                            intent.putExtra("CONOCE_CONTRASENA", tieneContrasena)
                            startActivity(intent)
                        } else {
                            // No tiene el fusible, le damos la pista
                            Toast.makeText(this, "🔌 Panel sin energía. Faltan los fusibles... Quizá haya refacciones en el Aula 9.", Toast.LENGTH_LONG).show()
                        }
                    }
                    "Aula 9" -> {
                        if (!tieneFusible) {
                            tieneFusible = true
                            Toast.makeText(this, "🎒 ¡Encontraste un Fusible de 50A en una caja de herramientas!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Solo hay cables viejos aquí.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Aula 7" -> {
                        if (!tieneContrasena) {
                            tieneContrasena = true
                            Toast.makeText(this, "📝 Lees en el pizarrón: 'No olvidar, clave del panel A2: 1936'", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "📝 Pizarrón: 'Clave del panel A2: 1936'", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Aula 4", "Aula 3", "Aula 5", "Aula 8" -> {
                        Toast.makeText(this, "Estás en $room. Todo se ve normal por aquí.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val (x, y) = playerPosition
                        Toast.makeText(this, "📍 $room (X=$x, Y=$y)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        btnBack.setOnClickListener { returnToEsime() }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) movementManager.handleMovement(event, deltaX, deltaY)
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            mapView.updateLocalPlayerPosition(position, forceCenter = true)
            if (::serverConnectionManager.isInitialized) {
                serverConnectionManager.sendUpdateMessage(playerName, position, "edificio3")
            }
            checkPositionForExit(position)
        }
    }

    private fun checkPositionForExit(position: Pair<Int, Int>) {
        // Solo permitir salida del edificio en PLANTA BAJA (currentFloor == 0)
        canExitBuilding = if (currentFloor == 0) {
            (position.first <= 4 || position.first >= 36) && position.second in 19..21
        } else {
            false  // No se puede salir en pisos superiores
        }
        
        if (canExitBuilding) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para salir del edificio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToEsime() {
        val intent = Intent(this, Esime::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", Pair(8, 17))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (::mapView.isInitialized) mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun initializeViews() {
        btnNorth = findViewById(R.id.btnNorth)
        btnSouth = findViewById(R.id.btnSouth)
        btnEast = findViewById(R.id.btnEast)
        btnWest = findViewById(R.id.btnWest)
        buttonA = findViewById(R.id.buttonA)
        btnBack = findViewById(R.id.btnBack)
        btnBack.text = "Volver a ESIME"
    }
    // 📍 CONFIGURAR VISUAL DE ESCALERAS EN EL MAPA
    private fun setupStairsVisual() {
        mapView.setCustomDrawCallback(object : MapView.CustomDrawCallback {
            override fun onCustomDraw(canvas: android.graphics.Canvas, cellWidth: Float, cellHeight: Float) {
                // Dibujar recuadro y punto indicador en las escaleras
                val paint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f
                    color = android.graphics.Color.GREEN // Color verde para indicar interacción
                    isAntiAlias = true
                }

                val (stairsX, stairsY) = stairsCenterPosition
                val x = stairsX * cellWidth
                val y = stairsY * cellHeight

                // Dibujar recuadro alrededor de la celda
                canvas.drawRect(
                    x,
                    y,
                    x + cellWidth,
                    y + cellHeight,
                    paint
                )

                // Dibujar círculo puntero adicional en el centro
                val circleRadius = cellWidth / 4
                val circlePaint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.FILL
                    color = android.graphics.Color.GREEN
                    isAntiAlias = true
                }
                canvas.drawCircle(
                    x + cellWidth / 2,
                    y + cellHeight / 2,
                    circleRadius,
                    circlePaint
                )

                // Dibujar símbolo de escaleras (dos líneas diagonales)
                val linePaint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 2f
                    color = android.graphics.Color.GREEN
                    isAntiAlias = true
                }
                val padding = cellWidth / 4
                // Primera línea diagonal
                canvas.drawLine(
                    x + padding,
                    y + cellHeight - padding,
                    x + cellWidth - padding,
                    y + padding,
                    linePaint
                )
                // Segunda línea diagonal paralela
                canvas.drawLine(
                    x + padding * 1.5f,
                    y + cellHeight - padding,
                    x + cellWidth - padding * 0.5f,
                    y + padding,
                    linePaint
                )
            }
        })
    }
    // 🟢 MOSTRAR MENÚ DE ESCALERAS PARA ELEGIR A QUÉ PISO IR
    private fun showStairsMenu() {
        val options = mutableListOf<String>()

        // Opción para bajar
        if (currentFloor > 0) {
            options.add("Bajar a Piso ${currentFloor - 1}")
        }

        // Opción para subir
        if (currentFloor < maxFloors) {
            options.add("Subir a Piso ${currentFloor + 1}")
        }

        if (options.isEmpty()) {
            Toast.makeText(this, "⛔ No puedes ir a otro piso desde aquí.", Toast.LENGTH_SHORT).show()
            return
        }

        val optionsArray = options.toTypedArray()
        var selectedIndex = 0

        // Usa Android AlertDialog para seleccionar piso
        android.app.AlertDialog.Builder(this)
            .setTitle("📍 Pisos disponibles (Piso Actual: $currentFloor)")
            .setSingleChoiceItems(optionsArray, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Ir") { _, _ ->
                val selectedOption = optionsArray[selectedIndex]
                val targetFloor = if (selectedOption.contains("Bajar")) {
                    currentFloor - 1
                } else {
                    currentFloor + 1
                }
                changeFloor(targetFloor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // 🟢 CAMBIAR DE PISO
    private fun changeFloor(newFloor: Int) {
        if (newFloor < 0 || newFloor > maxFloors) {
            Toast.makeText(this, "❌ Piso inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        currentFloor = newFloor
        updateFloorMap(currentFloor)
        updateFloorDisplay()

        Toast.makeText(this, "📍 Ahora estás en el Piso $currentFloor", Toast.LENGTH_SHORT).show()
        Log.d("Edificio3", "✅ Cambio a piso $currentFloor")
    }

    // 🟢 ACTUALIZAR EL MAPA VISUAL DEL PISO
    private fun updateFloorMap(floor: Int) {
        val mapResourceId = floorMaps[floor] ?: R.drawable.mapa_edificio3_esime
        mapView.setCurrentMap("edificio3_piso_$floor", mapResourceId)
        mapView.playerManager.setCurrentMap("edificio3_piso_$floor")
        mapView.invalidate()
    }

    // 🟢 ACTUALIZAR LA VISUALIZACIÓN DEL PISO ACTUAL
    private fun updateFloorDisplay() {
        val floorName = when (currentFloor) {
            0 -> "Planta Baja"
            else -> "Piso $currentFloor"
        }
        Log.d("Edificio3", "🏗️ Piso actual: $floorName")
        // Aquí puedes agregar un TextView en el layout para mostrar el piso si lo deseas
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val json = JSONObject(message)
                if (json.getString("type") in listOf("positions", "update")) {
                    val id = json.getString("id")
                    if (id != playerName) {
                        mapView.updateRemotePlayerPosition(id, Pair(json.getInt("x"), json.getInt("y")), json.getString("map"))
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (::movementManager.isInitialized) movementManager.setPosition(playerPosition)
        updateFloorDisplay()
    }

    override fun onPause() {
        super.onPause()
        if (::movementManager.isInitialized) movementManager.stopMovement()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) mapView.playerManager.cleanup()
    }

    override fun onMapTransitionRequested(targetMap: String, pos: Pair<Int, Int>) {
        if (targetMap == MapMatrixProvider.MAP_ESIME) returnToEsime()
    }
}