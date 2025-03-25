package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.buildingNumber3.pacman

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log

/**
 * Handles the rendering of Pacman game entities on the map
 */
class PacmanEntityRenderer {
    // Paint objects for different entities
    private val pacmanPaint = Paint().apply {
        color = Color.rgb(255, 255, 0) // Amarillo brillante para Pacman
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.rgb(255, 165, 0)) // Sombra naranja
    }

    private val ghostPaints = mapOf(
        "ghost_0" to Paint().apply { color = Color.RED; style = Paint.Style.FILL; isAntiAlias = true },
        "ghost_1" to Paint().apply { color = Color.rgb(255, 184, 255); style = Paint.Style.FILL; isAntiAlias = true }, // Pink
        "ghost_2" to Paint().apply { color = Color.CYAN; style = Paint.Style.FILL; isAntiAlias = true },
        "ghost_3" to Paint().apply { color = Color.rgb(255, 184, 82); style = Paint.Style.FILL; isAntiAlias = true } // Orange
    )

    private val foodPaint = Paint().apply {
        color = Color.rgb(255, 223, 0) // Amarillo brillante para la comida
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.WHITE) // Sombra blanca
    }

    private val ghostEyePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val ghostPupilPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val debugPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var pacmanMouthAngle = 45f
    private var mouthOpening = true
    private var lastAnimationTime = System.currentTimeMillis()

    private val ANIMATION_INTERVAL = 50L // milliseconds between animation updates
    private val MOUTH_ANGLE_STEP = 5f // degrees to open/close the mouth in each step
    /**
     * Draw a Pacman entity
     */
    fun drawPacman(canvas: Canvas, centerX: Float, centerY: Float, cellSize: Float, direction: Int) {
        try {
            // Animación más rápida del movimiento de la boca
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAnimationTime > ANIMATION_INTERVAL) {
                if (mouthOpening) {
                    pacmanMouthAngle += MOUTH_ANGLE_STEP
                    if (pacmanMouthAngle >= 45) mouthOpening = false
                } else {
                    pacmanMouthAngle -= MOUTH_ANGLE_STEP
                    if (pacmanMouthAngle <= 5) mouthOpening = true
                }
                lastAnimationTime = currentTime
            }

            // Calcular rotación basada en la dirección
            val rotation = when (direction) {
                PacmanController.DIRECTION_RIGHT -> 0f
                PacmanController.DIRECTION_UP -> 270f
                PacmanController.DIRECTION_LEFT -> 180f
                PacmanController.DIRECTION_BOTTOM -> 90f
                else -> 0f
            }

            // Guardar el estado del canvas
            canvas.save()

            // Trasladar al centro y rotar
            canvas.translate(centerX, centerY)
            canvas.rotate(rotation)
            canvas.translate(-centerX, -centerY)

            // Radio mucho más grande (80% del tamaño de celda)
            val radius = cellSize * 0.8f

            // Dibujar el cuerpo de Pacman (círculo con boca)
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                pacmanMouthAngle.toFloat(),
                360 - (2 * pacmanMouthAngle),
                true,
                pacmanPaint
            )

            // Restaurar el estado del canvas
            canvas.restore()
        } catch (e: Exception) {
            Log.e("PacmanEntityRenderer", "Error dibujando pacman: ${e.message}")
        }
    }


    /**
     * Draw a ghost entity
     */
    fun drawGhost(canvas: Canvas, centerX: Float, centerY: Float, cellSize: Float, ghostId: String) {
        try {
            val paint = ghostPaints[ghostId] ?: ghostPaints["ghost_0"] ?: pacmanPaint

            // Usar un radio más grande (60% del tamaño de celda)
            val radius = cellSize * 0.6f

            // Dibujar cuerpo del fantasma (rectángulo redondeado con parte inferior ondulada)
            val rect = RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius * 0.8f
            )

            // Dibujar la parte principal del cuerpo (parte superior)
            canvas.drawRect(
                centerX - radius,
                centerY - radius * 0.2f,
                centerX + radius,
                centerY + radius * 0.5f,
                paint
            )

            // Dibujar la parte superior redondeada
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius * 0.6f,
                180f,
                180f,
                true,
                paint
            )

            // Dibujar la parte inferior ondulada
            val wavePath = android.graphics.Path()
            wavePath.moveTo(centerX - radius, centerY + radius * 0.5f)

            // Crear una parte inferior ondulada con 3 semicírculos
            val waveWidth = radius * 2 / 3
            val waveHeight = radius * 0.3f

            wavePath.addArc(
                centerX - radius,
                centerY + radius * 0.5f - waveHeight,
                centerX - radius + waveWidth,
                centerY + radius * 0.5f + waveHeight,
                0f,
                180f
            )

            wavePath.addArc(
                centerX - radius + waveWidth,
                centerY + radius * 0.5f - waveHeight,
                centerX - radius + (2 * waveWidth),
                centerY + radius * 0.5f + waveHeight,
                180f,
                180f
            )

            wavePath.addArc(
                centerX - radius + (2 * waveWidth),
                centerY + radius * 0.5f - waveHeight,
                centerX + radius,
                centerY + radius * 0.5f + waveHeight,
                0f,
                180f
            )

            wavePath.close()
            canvas.drawPath(wavePath, paint)

            // Dibujar ojos - hacerlos más grandes
            val eyeRadius = radius * 0.3f
            val eyeYPos = centerY - radius * 0.1f

            // Ojo izquierdo
            canvas.drawCircle(centerX - eyeRadius - (eyeRadius * 0.3f), eyeYPos, eyeRadius, ghostEyePaint)
            // Ojo derecho
            canvas.drawCircle(centerX + eyeRadius + (eyeRadius * 0.3f), eyeYPos, eyeRadius, ghostEyePaint)

            // Dibujar pupilas - hacerlas más grandes
            val pupilRadius = eyeRadius * 0.7f
            // Pupila izquierda
            canvas.drawCircle(centerX - eyeRadius - (eyeRadius * 0.3f), eyeYPos, pupilRadius, ghostPupilPaint)
            // Pupila derecha
            canvas.drawCircle(centerX + eyeRadius + (eyeRadius * 0.3f), eyeYPos, pupilRadius, ghostPupilPaint)

        } catch (e: Exception) {
            Log.e("PacmanEntityRenderer", "Error dibujando fantasma: ${e.message}")
        }
    }
    /**
     * Draw a food entity
     */
    fun drawFood(canvas: Canvas, centerX: Float, centerY: Float, cellSize: Float) {
        try {
            // Usar un tamaño mucho más grande para la comida (30% del tamaño de celda)
            val foodRadius = cellSize * 0.3f

            // Dibujar un círculo más grande y brillante
            val brightFoodPaint = Paint().apply {
                color = Color.rgb(255, 223, 0) // Amarillo brillante
                style = Paint.Style.FILL
                setShadowLayer(2f, 0f, 0f, Color.WHITE) // Añadir un brillo
            }

            canvas.drawCircle(centerX, centerY, foodRadius, brightFoodPaint)

            // Añadir un brillo interior para mayor visibilidad
            val highlightPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(centerX - foodRadius/4, centerY - foodRadius/4, foodRadius/4, highlightPaint)
        } catch (e: Exception) {
            Log.e("PacmanEntityRenderer", "Error dibujando comida: ${e.message}")
        }
    }
}