package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings

import android.content.Intent
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

class Edificio5Activity : AppCompatActivity(), OnlineServerManager.WebSocketListener, MapView.MapTransitionListener {

    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

    // 🎒 Inventario para los puzzles del Edificio 5
    private var tieneFormato = false
    private var tieneSello = false
    private var tramiteCompletado = false

    private lateinit var btnNorth: MaterialButton
    private lateinit var btnSouth: MaterialButton
    private lateinit var btnEast: MaterialButton
    private lateinit var btnWest: MaterialButton
    private lateinit var buttonA: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var playerName: String
    private var isServer: Boolean = false
    private var canExitBuilding = false

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
                mapView.setCurrentMap("edificio5", R.drawable.mapa_edificio3_esime)
                mapView.playerManager.apply {
                    setCurrentMap("edificio5")
                    localPlayerId = playerName
                    updateLocalPlayerPosition(playerPosition)
                }
                initializeManagers()
                setupButtonListeners()
            }
        } catch (e: Exception) {
            Log.e("Edificio5", "Error: ${e.message}")
            finish()
        }
    }

    private fun createCollisionMap(): Array<BooleanArray> {
        val cols = 40
        val rows = 40
        val collisions = Array(cols) { BooleanArray(rows) { false } }

        for (x in 0 until cols) {
            collisions[x][0] = true; collisions[x][39] = true
        }
        for (y in 0 until rows) {
            collisions[0][y] = true; collisions[39][y] = true
        }

        for (x in 3..37) {
            collisions[x][7] = true
            collisions[x][18] = true
            collisions[x][22] = true
            collisions[x][32] = true
        }

        for (y in 7..18) {
            collisions[7][y] = true
            collisions[8][y] = true
            collisions[13][y] = true
            collisions[19][y] = true
            collisions[25][y] = true
        }
        for (y in 7..17) { collisions[31][y] = true }

        for (y in 23..32) {
            collisions[8][y] = true
            collisions[15][y] = true
            collisions[22][y] = true
            collisions[30][y] = true
        }

        for (x in 3..7) {
            collisions[x][17] = false
            collisions[x][18] = false
            collisions[x][22] = false
        }

        collisions[8][18] = false;  collisions[9][18] = false
        collisions[14][18] = false; collisions[15][18] = false
        collisions[20][18] = false
        collisions[26][18] = false
        collisions[10][22] = false
        collisions[17][22] = false; collisions[18][22] = false
        collisions[24][22] = false; collisions[25][22] = false

        return collisions
    }

    private fun getRoomName(position: Pair<Int, Int>): String {
        val (x, y) = position
        if (y in 19..21) return "Pasillo Principal"
        if (y == 18 || y == 22) return "En la Puerta"
        if (x in 3..7 && (y in 8..17 || y in 23..31)) return "Vestíbulo"
        if (y in 8..17) {
            return when {
                x in 9..12  -> "Ventanilla de Servicios"
                x in 14..24 -> "Sala de Espera"
                x in 26..30 -> "Oficina de Control Escolar"
                x > 31      -> "Archivo Histórico"
                else        -> "Muro"
            }
        }
        if (y in 23..31) {
            return when {
                x in 9..14  -> "Caja"
                x in 16..21 -> "Sala de Titulación"
                x in 23..29 -> "Imprenta"
                x > 30      -> "Escaleras"
                else        -> "Muro"
            }
        }
        return "Desconocido (X=$x, Y=$y)"
    }

    private fun isValidPosition(position: Pair<Int, Int>): Boolean {
        val (x, y) = position
        if (x < 0 || x >= 40 || y < 0 || y >= 40) return false
        return !collisionMap[x][y]
    }

    private fun initializeManagers() {
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@Edificio5Activity)
        }
        serverConnectionManager = ServerConnectionManager(this, onlineServerManager)

        movementManager = MovementManager(mapView) { newPosition ->
            if (isValidPosition(newPosition)) {
                playerPosition = newPosition
                updatePlayerPosition(newPosition)
            } else {
                movementManager.setPosition(playerPosition)
            }
        }

        mapView.setMapTransitionListener(this)
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(playerPosition)
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener  { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener  { _, event -> handleMovement(event, -1, 0); true }

        buttonA.setOnClickListener {
            if (canExitBuilding) {
                returnToEsime()
            } else {
                val room = getRoomName(playerPosition)
                when (room) {
                    "Ventanilla de Servicios" -> {
                        if (tramiteCompletado) {
                            Toast.makeText(this, "✅ Tu trámite ya fue procesado exitosamente.", Toast.LENGTH_SHORT).show()
                        } else if (tieneFormato && tieneSello) {
                            tramiteCompletado = true
                            Toast.makeText(this, "🎓 ¡Trámite completado! Tu certificado está listo.", Toast.LENGTH_LONG).show()
                        } else if (tieneFormato) {
                            Toast.makeText(this, "📋 El formato está bien, pero necesitas el sello de Caja.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "📄 Necesitas llenar el formato oficial. Ve a Imprenta.", Toast.LENGTH_LONG).show()
                        }
                    }
                    "Imprenta" -> {
                        if (!tieneFormato) {
                            tieneFormato = true
                            Toast.makeText(this, "📄 ¡Obtuviste el Formato de Solicitud de Certificado!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Ya tienes tu formato.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Caja" -> {
                        if (!tieneSello && tieneFormato) {
                            tieneSello = true
                            Toast.makeText(this, "🔏 ¡El cajero selló tu formato. Ahora ve a la Ventanilla!", Toast.LENGTH_LONG).show()
                        } else if (!tieneFormato) {
                            Toast.makeText(this, "Primero necesitas el formato de Imprenta.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Tu formato ya está sellado.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Sala de Espera", "Oficina de Control Escolar", "Sala de Titulación", "Archivo Histórico" -> {
                        Toast.makeText(this, "Estás en $room.", Toast.LENGTH_SHORT).show()
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
                serverConnectionManager.sendUpdateMessage(playerName, position, "edificio5")
            }
            checkPositionForExit(position)
        }
    }

    private fun checkPositionForExit(position: Pair<Int, Int>) {
        canExitBuilding = (position.first <= 4 || position.first >= 36) && position.second in 19..21
        if (canExitBuilding) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para salir del Edificio 5", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToEsime() {
        val intent = Intent(this, Esime::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", Pair(8, 5))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (::mapView.isInitialized) mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun initializeViews() {
        btnNorth = findViewById(R.id.btnNorth)
        btnSouth = findViewById(R.id.btnSouth)
        btnEast  = findViewById(R.id.btnEast)
        btnWest  = findViewById(R.id.btnWest)
        buttonA  = findViewById(R.id.buttonA)
        btnBack  = findViewById(R.id.btnBack)
        btnBack.text = "Volver a ESIME"
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
            } catch (e: Exception) { }
        }
    }

    override fun onResume()  { super.onResume();  if (::movementManager.isInitialized) movementManager.setPosition(playerPosition) }
    override fun onPause()   { super.onPause();   if (::movementManager.isInitialized) movementManager.stopMovement() }
    override fun onDestroy() { super.onDestroy(); if (::mapView.isInitialized) mapView.playerManager.cleanup() }
    override fun onMapTransitionRequested(targetMap: String, pos: Pair<Int, Int>) {
        if (targetMap == MapMatrixProvider.MAP_ESIME) returnToEsime()
    }
}
