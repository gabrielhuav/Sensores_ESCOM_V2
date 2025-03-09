package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.os.Handler
import android.os.Looper
import android.util.Log
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
        try {
            val newX = (localPlayerPosition.first + deltaX).coerceIn(0, MapMatrixProvider.MAP_WIDTH - 1)
            val newY = (localPlayerPosition.second + deltaY).coerceIn(0, MapMatrixProvider.MAP_HEIGHT - 1)

            if (mapView.isValidPosition(newX, newY)) {
                // Solo actualizamos si la posición es diferente (para evitar actualizaciones innecesarias)
                if (localPlayerPosition.first != newX || localPlayerPosition.second != newY) {
                    localPlayerPosition = Pair(newX, newY)

                    // Actualizar la posición y forzar centrado
                    mapView.updateLocalPlayerPosition(localPlayerPosition, forceCenter = true)

                    // Notificar a la actividad
                    onPositionUpdate(localPlayerPosition)
                }
            }
        } catch (e: Exception) {
            Log.e("MovementManager", "Error en movePlayer: ${e.message}")
        }
    }
    // Método para añadir a la clase MovementManager
// Agregar estas funciones después de las existentes en MovementManager.kt

    /**
     * Lista de tareas programadas
     */
    private val scheduledTasks = mutableListOf<Pair<Runnable, Long>>()

    /**
     * Programa una acción para ser ejecutada después de un tiempo
     *
     * @param delayMs Tiempo en milisegundos antes de ejecutar la acción
     * @param action Acción a ejecutar
     * @return Runnable que puede ser cancelado posteriormente
     */
    fun scheduleDelayedAction(delayMs: Long, action: () -> Unit): Runnable {
        val runnable = Runnable { action() }
        handler.postDelayed(runnable, delayMs)

        val taskPair = Pair(runnable, System.currentTimeMillis() + delayMs)
        scheduledTasks.add(taskPair)

        return runnable
    }

    /**
     * Cancela una acción programada
     */
    fun cancelScheduledAction(runnable: Runnable) {
        handler.removeCallbacks(runnable)
        scheduledTasks.removeIf { it.first == runnable }
    }

    /**
     * Cancela todas las acciones programadas
     */
    fun cancelAllScheduledActions() {
        scheduledTasks.forEach { (runnable, _) ->
            handler.removeCallbacks(runnable)
        }
        scheduledTasks.clear()
    }

    /**
     * Sobrecarga de stopMovement para limpiar también acciones programadas
     */
    fun stopMovementAndCancelActions() {
        stopMovement()
        cancelAllScheduledActions()
    }
}