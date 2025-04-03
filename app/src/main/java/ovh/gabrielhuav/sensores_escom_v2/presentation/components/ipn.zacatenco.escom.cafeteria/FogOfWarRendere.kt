package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.cafeteria

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Log
import kotlin.math.min

/**
 * Renderizador de la niebla de guerra (fog of war) para el minijuego de zombies
 * Este componente añade una capa oscura al mapa y revela el área alrededor del jugador
 */
class FogOfWarRenderer {
    companion object {
        private const val TAG = "FogOfWarRenderer"

        // Constantes para ajustar la apariencia de la niebla
        private const val FOG_ALPHA = 180 // Opacidad de la niebla (0-255)
        private const val VISION_RADIUS_MULTIPLIER = 1.0f // Multiplicador del radio de visión
    }

    // Paint para la capa de niebla
    private val fogPaint = Paint().apply {
        color = Color.BLACK
        alpha = FOG_ALPHA
        style = Paint.Style.FILL
    }

    // Paint para crear el efecto de claridad alrededor del jugador
    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        alpha = 0
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Paint para el gradiente radial (efecto de lámpara)
    private var gradientPaint: Paint? = null

    /**
     * Dibuja la capa de niebla de guerra
     */
    fun drawFogOfWar(
        canvas: Canvas,
        playerPosition: Pair<Int, Int>,
        cellWidth: Float,
        cellHeight: Float,
        visionRadius: Int
    ) {
        try {
            // Guardar el estado actual del canvas
            val saveCount = canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), null)

            // Calcular posición del jugador en píxeles
            val playerX = playerPosition.first * cellWidth + (cellWidth / 2)
            val playerY = playerPosition.second * cellHeight + (cellHeight / 2)

            // Calcular radio de visión en píxeles
            val visionRadiusPixels = visionRadius * min(cellWidth, cellHeight) * VISION_RADIUS_MULTIPLIER

            // Dibujar la capa de niebla completa
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), fogPaint)

            // Inicializar el paint con gradiente radial si no existe
            if (gradientPaint == null) {
                gradientPaint = Paint().apply {
                    shader = RadialGradient(
                        playerX, playerY, visionRadiusPixels,
                        intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
                        floatArrayOf(0.7f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                    style = Paint.Style.FILL
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
            } else {
                // Actualizar la posición del gradiente
                gradientPaint?.shader = RadialGradient(
                    playerX, playerY, visionRadiusPixels,
                    intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
                    floatArrayOf(0.7f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }

            // Crear un círculo claro alrededor del jugador
            canvas.drawCircle(playerX, playerY, visionRadiusPixels * 0.7f, clearPaint)

            // Aplicar el gradiente para suavizar los bordes
            canvas.drawCircle(playerX, playerY, visionRadiusPixels, gradientPaint!!)

            // Restaurar el canvas
            canvas.restoreToCount(saveCount)

        } catch (e: Exception) {
            Log.e(TAG, "Error dibujando niebla de guerra: ${e.message}")
        }
    }

    /**
     * Determina si una entidad está visible para el jugador
     */
    fun isEntityVisible(
        entityPosition: Pair<Int, Int>,
        playerPosition: Pair<Int, Int>,
        visionRadius: Int
    ): Boolean {
        val dx = entityPosition.first - playerPosition.first
        val dy = entityPosition.second - playerPosition.second
        val distanceSquared = dx * dx + dy * dy

        // Comparamos con el cuadrado del radio para evitar calcular raíz cuadrada
        return distanceSquared <= visionRadius * visionRadius
    }
}