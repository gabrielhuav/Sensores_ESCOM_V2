package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent

class MovementManager(
    private val mapView: MapView,
    private val onPositionUpdate: (Pair<Int, Int>) -> Unit
) {
    private var localPlayerPosition = Pair(1, 1)
    private val handler = Handler(Looper.getMainLooper())
    private var currentMovementRunnable: Runnable? = null

    fun setPosition(position: Pair<Int, Int>) {
        localPlayerPosition = position
        mapView.updateLocalPlayerPosition(position)
    }

    fun getCurrentPosition(): Pair<Int, Int> = localPlayerPosition

    fun stopMovement() {
        currentMovementRunnable?.let { handler.removeCallbacks(it) }
        currentMovementRunnable = null
    }

    fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                stopMovement()
                currentMovementRunnable = createMovementRunnable(deltaX, deltaY).also {
                    handler.post(it)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopMovement()
            }
        }
    }

    private fun createMovementRunnable(deltaX: Int, deltaY: Int) = object : Runnable {
        override fun run() {
            movePlayer(deltaX, deltaY)
            handler.postDelayed(this, 100)
        }
    }

    private fun movePlayer(deltaX: Int, deltaY: Int) {
        val newX = (localPlayerPosition.first + deltaX).coerceIn(0, MapMatrixProvider.MAP_WIDTH - 1)
        val newY = (localPlayerPosition.second + deltaY).coerceIn(0, MapMatrixProvider.MAP_HEIGHT - 1)

        if (mapView.isValidPosition(newX, newY)) {
            localPlayerPosition = Pair(newX, newY)
            mapView.updateLocalPlayerPosition(localPlayerPosition)
            onPositionUpdate(localPlayerPosition)
        }
    }
}