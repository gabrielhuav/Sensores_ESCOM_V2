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

class CafeteriaActivity : AppCompatActivity(), OnlineServerManager.WebSocketListener {

    private lateinit var onlineServerManager: OnlineServerManager
    private lateinit var playerName: String
    private lateinit var mapView: MapView
    private var currentPosition: Pair<Int, Int> = Pair(0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafeteria)

        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Error: Nombre del jugador no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentPosition = intent.getSerializableExtra("PLAYER_POSITION") as? Pair<Int, Int> ?: Pair(0, 0)

        println("CafeteriaActivity - playerName: $playerName")
        println("CafeteriaActivity - initialPosition: $currentPosition")

        onlineServerManager = OnlineServerManager(this)
        onlineServerManager.connectToServer("ws://192.168.1.17:3000")

        setupMapView(Pair(0, 0))
        setupMovementButtons()
        setupBackButton()
    }

    private fun setupMapView(initialPosition: Pair<Int, Int>) {
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_cafeteria)
        mapView = MapView(this, null).apply {
            setCafeteriaMatrix() // Necesitarás añadir este método en MapView
            setMapResource(R.drawable.escom_cafeteria) // Necesitarás añadir esta imagen
            updateLocalPlayerPosition(initialPosition)
        }
        mapContainer.addView(mapView)

        Handler(Looper.getMainLooper()).postDelayed({
            onlineServerManager.sendUpdateMessage(playerName, initialPosition.first, initialPosition.second, "cafeteria")
        }, 1000)
    }

    private fun setupMovementButtons() {
        findViewById<Button>(R.id.button_move_north).setOnClickListener {
            movePlayer(0, -1)
        }
        findViewById<Button>(R.id.button_move_south).setOnClickListener {
            movePlayer(0, 1)
        }
        findViewById<Button>(R.id.button_move_east).setOnClickListener {
            movePlayer(1, 0)
        }
        findViewById<Button>(R.id.button_move_west).setOnClickListener {
            movePlayer(-1, 0)
        }
    }

    private fun movePlayer(deltaX: Int, deltaY: Int) {
        val newPosition = mapView.localPlayerPosition?.let { (x, y) ->
            Pair(x + deltaX, y + deltaY)
        }
        if (newPosition != null) {
            mapView.updateLocalPlayerPosition(newPosition)
            onlineServerManager.sendUpdateMessage(playerName, newPosition.first, newPosition.second, "cafeteria")
            currentPosition = newPosition
        }
    }

    private fun setupBackButton() {
        findViewById<Button>(R.id.button_back_to_gameplay).setOnClickListener {
            val intent = Intent(this, GameplayActivity::class.java)
            intent.putExtra("PLAYER_POSITION", Pair(25, 20)) // Coordenadas donde está la cafetería
            intent.putExtra("PLAYER_NAME", playerName)

            onlineServerManager.sendUpdateMessage(playerName, 25, 20, "main")

            startActivity(intent)
            finish()
        }
    }

    override fun onMessageReceived(message: String) {
        println("Mensaje recibido en CafeteriaActivity: $message")
    }
}