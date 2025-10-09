package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.locations.esime.buildings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class Edificio3Activity : AppCompatActivity(), OnlineServerManager.WebSocketListener, MapView.MapTransitionListener {

    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

    // Botones
    private lateinit var btnNorth: MaterialButton
    private lateinit var btnSouth: MaterialButton
    private lateinit var btnEast: MaterialButton
    private lateinit var btnWest: MaterialButton
    private lateinit var buttonA: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var playerName: String

    // 🔴 CAMBIADO: Posición inicial en el MEDIO del edificio
    private var playerPosition: Pair<Int, Int> = Pair(10, 10)  // Posición central
    private var isServer: Boolean = false
    private var canExitBuilding = false

    // 🔴 NUEVO: Mapa de colisiones basado en la forma real del edificio
    private val collisionMap = createCollisionMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esime) // Usa el mismo layout

        try {
            // Inicializar vistas
            initializeViews()

            // Inicializar mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.mapa_edificio3_esime // Crea este recurso
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Obtener datos del Intent
            playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"
            isServer = intent.getBooleanExtra("IS_SERVER", false)

            // 🔴 USAR POSICIÓN MEDIA SI NO VIENE DEL EXTERIOR
            playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                ?: Pair(10, 10)  // Posición central por defecto

            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa (puedes crear un mapa específico para el edificio 3)
                mapView.setCurrentMap("edificio3", R.drawable.mapa_edificio3_esime)

                // Configurar playerManager
                mapView.playerManager.apply {
                    setCurrentMap("edificio3")
                    localPlayerId = playerName
                    updateLocalPlayerPosition(playerPosition)
                }

                // Inicializar managers
                initializeManagers()

                // Configurar listeners de botones
                setupButtonListeners()

                Log.d("Edificio3", "Edificio 3 inicializado - Posición: $playerPosition")
            }
        } catch (e: Exception) {
            Log.e("Edificio3", "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando Edificio 3.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // 🔴 NUEVO: Crear mapa de colisiones basado en la forma real del edificio
    private fun createCollisionMap(): Array<BooleanArray> {
        // Definir las áreas transitables (false = se puede caminar, true = bloqueado)
        val collisions = Array(20) { BooleanArray(20) { true } } // Inicialmente todo bloqueado

        try {
            // Cargar el bitmap para analizar la forma
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.mapa_edificio3_esime)

            // 🔴 DEFINIR MANUALMENTE LAS ZONAS TRANSITABLES DEL EDIFICIO
            // Ajusta estas coordenadas según la forma real de tu imagen

            // Pasillos principales (forma de cruz)
            for (x in 5..15) {
                collisions[x][10] = false  // Pasillo horizontal central
            }
            for (y in 5..15) {
                collisions[10][y] = false  // Pasillo vertical central
            }

            // Aulas y salones (áreas rectangulares)
            for (x in 3..7) {
                for (y in 3..7) {
                    collisions[x][y] = false  // Aula noroeste
                }
            }
            for (x in 13..17) {
                for (y in 3..7) {
                    collisions[x][y] = false  // Aula noreste
                }
            }
            for (x in 3..7) {
                for (y in 13..17) {
                    collisions[x][y] = false  // Aula suroeste
                }
            }
            for (x in 13..17) {
                for (y in 13..17) {
                    collisions[x][y] = false  // Aula sureste
                }
            }

            // Área central amplia
            for (x in 8..12) {
                for (y in 8..12) {
                    collisions[x][y] = false  // Plaza central
                }
            }

            // Pasillos adicionales
            for (x in 2..4) {
                collisions[x][10] = false  // Extensión oeste
            }
            for (x in 16..18) {
                collisions[x][10] = false  // Extensión este
            }

            bitmap?.recycle()

        } catch (e: Exception) {
            Log.e("Edificio3", "Error creando mapa de colisiones: ${e.message}")
            // Fallback: área central transitable
            for (x in 5..15) {
                for (y in 5..15) {
                    collisions[x][y] = false
                }
            }
        }

        return collisions
    }

    // 🔴 NUEVO: Verificar si una posición es válida según la forma del edificio
    private fun isValidPosition(position: Pair<Int, Int>): Boolean {
        val (x, y) = position

        // Verificar límites del mapa
        if (x < 0 || x >= collisionMap.size || y < 0 || y >= collisionMap[0].size) {
            return false
        }

        // Verificar colisión según el mapa
        return !collisionMap[x][y]
    }

    private fun initializeViews() {
        btnNorth = findViewById(R.id.btnNorth)
        btnSouth = findViewById(R.id.btnSouth)
        btnEast = findViewById(R.id.btnEast)
        btnWest = findViewById(R.id.btnWest)
        buttonA = findViewById(R.id.buttonA)
        btnBack = findViewById(R.id.btnBack)

        // 🔴 CAMBIAR EL TEXTO DEL BOTÓN BACK PARA INDICAR QUE REGRESA A ESIME
        btnBack.text = "Volver a ESIME"
    }

    private fun initializeManagers() {
        // Configurar OnlineServerManager
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@Edificio3Activity)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        // 🔴 CORREGIDO: MovementManager sin collisionChecker
        movementManager = MovementManager(
            mapView = mapView
        ) { position ->
            // 🔴 AQUÍ VERIFICAMOS LAS COLISIONES ANTES DE ACTUALIZAR
            if (isValidPosition(position)) {
                updatePlayerPosition(position)
            } else {
                // Feedback de colisión
                Log.d("EDIFICIO3_COLLISION", "Movimiento bloqueado en: $position")
                // Opcional: vibración o sonido
                // Toast.makeText(this, "No puedes pasar por ahí", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el listener de transición
        mapView.setMapTransitionListener(this)

        // Establecer ID del jugador local
        mapView.playerManager.localPlayerId = playerName

        // Posición inicial
        updatePlayerPosition(playerPosition)
    }

    private fun setupButtonListeners() {
        // Controles de movimiento
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        // Botón A para interactuar
        buttonA.setOnClickListener {
            if (canExitBuilding) {
                returnToEsime()
            } else {
                // 🔴 NUEVO: Mostrar información de la posición actual
                showPositionInfo(playerPosition)
            }
        }

        // 🔴🔴🔴 BOTÓN BACK MODIFICADO PARA REGRESAR A ESIME 🔴🔴🔴
        btnBack.setOnClickListener {
            returnToEsime()
        }
    }

    // 🔴 NUEVO: Mostrar información de la posición actual
    private fun showPositionInfo(position: Pair<Int, Int>) {
        val (x, y) = position

        // Determinar en qué área está el jugador
        val area = when {
            x in 3..7 && y in 3..7 -> "Aula Noroeste"
            x in 13..17 && y in 3..7 -> "Aula Noreste"
            x in 3..7 && y in 13..17 -> "Aula Suroeste"
            x in 13..17 && y in 13..17 -> "Aula Sureste"
            x == 10 && y == 10 -> "Plaza Central"
            x in 5..15 && y == 10 -> "Pasillo Principal Horizontal"
            x == 10 && y in 5..15 -> "Pasillo Principal Vertical"
            else -> "Área del Edificio 3"
        }

        Toast.makeText(this, area, Toast.LENGTH_SHORT).show()
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            movementManager.handleMovement(event, deltaX, deltaY)
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                playerPosition = position
                mapView.updateLocalPlayerPosition(position, forceCenter = true)

                if (::serverConnectionManager.isInitialized) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, "edificio3")
                }

                checkPositionForExit(position)

                // 🔴 DEBUG: Mostrar posición actual
                Log.d("EDIFICIO3_POS", "Posición actual: $position - Válida: ${isValidPosition(position)}")

            } catch (e: Exception) {
                Log.e("Edificio3", "Error en updatePlayerPosition: ${e.message}")
            }
        }
    }

    private fun checkPositionForExit(position: Pair<Int, Int>) {
        // 🔴 MEJORADO: Salidas en los bordes del área transitable
        val exitPositions = listOf(
            Pair(2, 10),   // Salida Oeste
            Pair(18, 10),  // Salida Este
            Pair(10, 2),   // Salida Norte
            Pair(10, 18)   // Salida Sur
        )

        canExitBuilding = exitPositions.any { exitPos ->
            position.first == exitPos.first && position.second == exitPos.second
        }

        if (canExitBuilding) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para salir del Edificio 3", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToEsime() {
        val intent = Intent(this, Esime::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", Pair(8, 17)) // 🔴 Posición frente al edificio 3 en ESIME
            putExtra("PREVIOUS_POSITION", playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
        startActivity(intent)
        finish()

        Toast.makeText(this, "Regresando a ESIME", Toast.LENGTH_SHORT).show()
    }

    // WebSocket listener
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                when (jsonObject.getString("type")) {
                    "positions", "update" -> {
                        val playerId = jsonObject.getString("id")
                        if (playerId != playerName) {
                            val position = Pair(
                                jsonObject.getInt("x"),
                                jsonObject.getInt("y")
                            )
                            val map = jsonObject.getString("map")
                            mapView.updateRemotePlayerPosition(playerId, position, map)
                        }
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e("Edificio3", "Error procesando mensaje: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::movementManager.isInitialized) {
            movementManager.setPosition(playerPosition)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::movementManager.isInitialized) {
            movementManager.stopMovement()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
    }

    // Manejar transición de mapa
    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_ESIME -> {
                returnToEsime()
            }
            else -> {
                Log.d("Edificio3", "Mapa destino no reconocido: $targetMap")
            }
        }
    }

    companion object {
        private const val TAG = "Edificio3"
    }
}