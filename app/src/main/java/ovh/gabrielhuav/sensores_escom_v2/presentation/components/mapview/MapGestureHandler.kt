package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MapGestureHandler(private val view: View) {
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var bitmap: Bitmap? = null
    private var callback: GestureCallback? = null

    private val MIN_SCALE = 0.8f    // Mínima escala permitida (80%)
    private val MAX_SCALE = 2.0f    // Máxima escala permitida (200%)
    private var currentScale = 1.0f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var isScaling = false

    interface GestureCallback {
        fun onOffsetChanged(offsetX: Float, offsetY: Float)
        fun onScaleChanged(scaleFactor: Float)
        fun invalidateView()
        fun constrainOffset() // Nuevo método para restringir offset
    }

    fun initializeDetectors(context: Context) {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isScaling) {
                    callback?.let { cb ->
                        // Invertir las distancias para un movimiento natural
                        cb.onOffsetChanged(-distanceX, -distanceY)
                        // Asegurar que no nos salimos de los límites
                        cb.constrainOffset()
                        cb.invalidateView()
                    }
                }
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Doble toque para resetear la escala y centrar en el jugador
                currentScale = 1.0f
                callback?.onScaleChanged(1.0f)
                callback?.constrainOffset()
                callback?.invalidateView()
                return true
            }
        })

        scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    isScaling = true
                    lastFocusX = detector.focusX
                    lastFocusY = detector.focusY
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    currentScale *= scaleFactor
                    currentScale = currentScale.coerceIn(MIN_SCALE, MAX_SCALE)

                    callback?.let { cb ->
                        // Calcular el desplazamiento basado en el punto focal
                        val focusX = detector.focusX
                        val focusY = detector.focusY

                        // Calcular el desplazamiento del punto focal
                        val focusShiftX = focusX - lastFocusX
                        val focusShiftY = focusY - lastFocusY

                        // Actualizar el último punto focal
                        lastFocusX = focusX
                        lastFocusY = focusY

                        // Aplicar la escala
                        cb.onScaleChanged(currentScale)

                        // Aplicar el desplazamiento si hay cambio en el punto focal
                        if (focusShiftX != 0f || focusShiftY != 0f) {
                            cb.onOffsetChanged(focusShiftX, focusShiftY)
                        }

                        // Asegurar que no nos salimos de los límites
                        cb.constrainOffset()
                        cb.invalidateView()
                    }
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    isScaling = false
                    callback?.constrainOffset() // Restringir al finalizar el zoom
                }
            })
    }



    fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            // Manejar el final del gesto
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isScaling = false
                callback?.constrainOffset() // Restringir al soltar
            }

            var handled = scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                handled = gestureDetector.onTouchEvent(event) || handled
            }

            return handled
        } catch (e: Exception) {
            Log.e("MapGestureHandler", "Error en onTouchEvent: ${e.message}")
            return false
        }
    }

    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }

    fun setCallback(callback: GestureCallback) {
        this.callback = callback
    }
}
