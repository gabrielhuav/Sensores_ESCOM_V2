package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

class BuildingActivity : AppCompatActivity(), OnlineServerManager.WebSocketListener {

    private lateinit var onlineServerManager: OnlineServerManager
    private lateinit var playerName: String
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_building)

        // Recuperar el nombre del jugador
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Error: Nombre del jugador no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // El jugador empieza siempre en (0, 0) al ingresar al edificio
        val initialPosition = Pair(0, 0)

        println("BuildingActivity - playerName: $playerName")
        println("BuildingActivity - initialPosition: $initialPosition")

        // Inicializar el servidor
        onlineServerManager = OnlineServerManager(this)
        onlineServerManager.connectToServer("ws://192.168.1.31:3000")

        // Configurar el MapView
        setupMapView(initialPosition)

        // Configurar los botones
        setupMovementButtons()
        setupBackButton()
    }

    private fun setupMapView(initialPosition: Pair<Int, Int>) {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_building)
        mapView = MapView(this).apply {
            setBuildingMatrix() // Configura la matriz específica del edificio
            setMapResource(R.drawable.escom_edificio3) // Configura el recurso del mapa
            updateLocalPlayerPosition(initialPosition) // Actualiza la posición inicial del jugador
        }
        mapContainer.addView(mapView)

        // Enviar posición inicial al servidor
        Handler(Looper.getMainLooper()).postDelayed({
            onlineServerManager.sendUpdateMessage(playerName, initialPosition.first, initialPosition.second, "building")
        }, 1000)
    }

    private fun setupMovementButtons() {
        findViewById<Button>(R.id.button_move_north).setOnClickListener {
            movePlayer(0, -1) // Mover hacia el norte
        }
        findViewById<Button>(R.id.button_move_south).setOnClickListener {
            movePlayer(0, 1) // Mover hacia el sur
        }
        findViewById<Button>(R.id.button_move_east).setOnClickListener {
            movePlayer(1, 0) // Mover hacia el este
        }
        findViewById<Button>(R.id.button_move_west).setOnClickListener {
            movePlayer(-1, 0) // Mover hacia el oeste
        }
    }

    private fun movePlayer(deltaX: Int, deltaY: Int) {
        val newPosition = mapView.localPlayerPosition?.let { (x, y) ->
            Pair(x + deltaX, y + deltaY)
        }
        if (newPosition != null) {
            mapView.updateLocalPlayerPosition(newPosition)
            onlineServerManager.sendUpdateMessage(playerName, newPosition.first, newPosition.second, "building")
        }
    }

    private fun setupBackButton() {
        findViewById<Button>(R.id.button_back_to_gameplay).setOnClickListener {
            val intent = Intent(this, GameplayActivity::class.java)

            // Al regresar al mapa principal, el jugador debe volver a (15, 10)
            val mainMapPosition = Pair(15, 10)
            intent.putExtra("PLAYER_POSITION", mainMapPosition)
            intent.putExtra("PLAYER_NAME", playerName)

            // Notificar al servidor antes de regresar
            onlineServerManager.sendUpdateMessage(playerName, mainMapPosition.first, mainMapPosition.second, "main")

            startActivity(intent)
            finish()
        }
    }

    override fun onMessageReceived(message: String) {
        println("Mensaje recibido en BuildingActivity: $message")
    }
}
