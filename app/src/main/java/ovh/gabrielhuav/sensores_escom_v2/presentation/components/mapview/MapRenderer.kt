package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log

class MapRenderer {
    private val paintInteractive = Paint().apply { color = Color.YELLOW }
    private val paintWall = Paint().apply { color = Color.BLACK }
    private val paintPath = Paint().apply { color = Color.WHITE }
    private val paintInaccessible = Paint().apply { color = Color.DKGRAY }
    private val paintBackground = Paint().apply { color = Color.WHITE }

    fun draw(canvas: Canvas, mapState: MapState, playerManager: PlayerManager) {
        // Dibujar fondo blanco
        canvas.drawColor(Color.WHITE)

        mapState.backgroundBitmap?.let { bitmap ->
            canvas.save()

            // Aplicar transformaciones para centrado
            val viewWidth = canvas.width.toFloat()
            val viewHeight = canvas.height.toFloat()
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            // Calcular offset para centrado
            val centerOffsetX = (viewWidth - bitmapWidth * mapState.scaleFactor) / 2
            val centerOffsetY = (viewHeight - bitmapHeight * mapState.scaleFactor) / 2

            // Aplicar transformaciones
            canvas.translate(centerOffsetX + mapState.offsetX, centerOffsetY + mapState.offsetY)
            canvas.scale(mapState.scaleFactor, mapState.scaleFactor)

            // Dibujar matriz del mapa
            drawMapMatrix(canvas, mapState, bitmap.width.toFloat(), bitmap.height.toFloat())

            // Dibujar el bitmap del mapa con cierta transparencia
            val alphaPaint = Paint().apply { alpha = 128 }
            canvas.drawBitmap(bitmap, 0f, 0f, alphaPaint)

            // Dibujar jugadores
            playerManager.drawPlayers(canvas, mapState)

            canvas.restore()
        } ?: run {
            drawErrorState(canvas)
        }
    }

    private fun drawMapMatrix(canvas: Canvas, mapState: MapState, width: Float, height: Float) {
        val cellWidth = width / mapState.mapMatrix[0].size
        val cellHeight = height / mapState.mapMatrix.size

        for (i in mapState.mapMatrix.indices) {
            for (j in mapState.mapMatrix[i].indices) {
                val paint = when (mapState.mapMatrix[i][j]) {
                    0 -> paintInteractive
                    1 -> paintWall
                    2 -> paintPath
                    3 -> paintInaccessible
                    else -> paintPath
                }
                canvas.drawRect(
                    j * cellWidth,
                    i * cellHeight,
                    (j + 1) * cellWidth,
                    (i + 1) * cellHeight,
                    paint
                )
            }
        }
    }

    private fun drawErrorState(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.RED
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Error: Mapa no cargado",
            canvas.width / 2f,
            canvas.height / 2f,
            paint
        )
    }
}