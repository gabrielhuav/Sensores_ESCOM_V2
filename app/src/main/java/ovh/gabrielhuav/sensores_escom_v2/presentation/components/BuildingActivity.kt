package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

class BuildingActivity : AppCompatActivity(), OnlineServerManager.WebSocketListener {

    private lateinit var onlineServerManager: OnlineServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_building)

        // Inicializa el administrador del servidor
        onlineServerManager = OnlineServerManager(this)
        onlineServerManager.connectToServer("ws://192.168.1.31:3000") // Asegúrate de conectar primero

        val initialPosition = intent.getSerializableExtra("PLAYER_POSITION") as? Pair<Int, Int> ?: Pair(0, 0)
        val playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"

        // Configura el MapView
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_building)
        val mapView = MapView(this).apply {
            setBuildingMatrix()
            setMapResource(R.drawable.escom_edificio3)
            updateLocalPlayerPosition(initialPosition)
        }
        mapContainer.addView(mapView)

        // Esperar conexión antes de enviar mensajes
        Handler(Looper.getMainLooper()).postDelayed({
            onlineServerManager.sendUpdateMessage(playerName, initialPosition.first, initialPosition.second, "building")
        }, 1000)

        setupMovementButtons(mapView)
        setupBackButton(mapView, playerName)
    }


    override fun onMessageReceived(message: String) {
        // Maneja los mensajes recibidos del servidor aquí
        println("Mensaje recibido en BuildingActivity: $message")
    }

    private fun setupMovementButtons(mapView: MapView) {
        findViewById<Button>(R.id.button_move_north).setOnClickListener {
            movePlayer(mapView, 0, -1) // Mover hacia el norte
        }
        findViewById<Button>(R.id.button_move_south).setOnClickListener {
            movePlayer(mapView, 0, 1) // Mover hacia el sur
        }
        findViewById<Button>(R.id.button_move_east).setOnClickListener {
            movePlayer(mapView, 1, 0) // Mover hacia el este
        }
        findViewById<Button>(R.id.button_move_west).setOnClickListener {
            movePlayer(mapView, -1, 0) // Mover hacia el oeste
        }
    }

    private fun movePlayer(mapView: MapView, deltaX: Int, deltaY: Int) {
        mapView.updateLocalPlayerPosition(
            mapView.localPlayerPosition?.let { (x, y) -> Pair(x + deltaX, y + deltaY) }
        )
    }

    private fun setupBackButton(mapView: MapView, playerName: String) {
        findViewById<Button>(R.id.button_back_to_gameplay).setOnClickListener {
            val intent = Intent(this, GameplayActivity::class.java)
            intent.putExtra("PLAYER_POSITION", mapView.localPlayerPosition)
            intent.putExtra("PLAYER_NAME", playerName)

            // Actualiza el mapa en el servidor
            onlineServerManager.sendUpdateMessage(playerName, mapView.localPlayerPosition?.first ?: 0, mapView.localPlayerPosition?.second ?: 0, "main")

            startActivity(intent)
            finish()
        }
    }
}
