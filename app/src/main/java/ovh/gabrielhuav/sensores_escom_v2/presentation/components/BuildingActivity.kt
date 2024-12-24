package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R

class BuildingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_building)

        // Agregar MapView dinámicamente al contenedor
        val mapContainer = findViewById<FrameLayout>(R.id.map_container_building)
        val mapView = MapView(this).apply {
            setBuildingMatrix() // Configurar el mapa del edificio
        }
        mapContainer.addView(mapView)

        setupMovementButtons(mapView)
        setupBackButton()
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
        // Lógica para mover al jugador dentro del edificio.
        mapView.updateLocalPlayerPosition(
            mapView.localPlayerPosition?.let { (x, y) -> Pair(x + deltaX, y + deltaY) }
        )
    }

    private fun setupBackButton() {
        findViewById<Button>(R.id.button_back_to_gameplay).setOnClickListener {
            val intent = Intent(this, GameplayActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Cerrar el `Activity` actual para liberar recursos
        }
    }
}
