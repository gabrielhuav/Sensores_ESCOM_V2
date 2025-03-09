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
        try {
            // Dibujar fondo blanco
            canvas.drawColor(Color.WHITE)

            mapState.backgroundBitmap?.let { bitmap ->
                canvas.save()

                // Paso 1: Aplicar transformaciones al canvas
                canvas.translate(mapState.offsetX, mapState.offsetY)
                canvas.scale(mapState.scaleFactor, mapState.scaleFactor)

                // Paso 2: Dibujar la matriz del mapa PRIMERO
                // Esto asegura que se vea debajo del bitmap
                mapState.drawMapMatrix(canvas, bitmap.width.toFloat(), bitmap.height.toFloat())

                // Paso 3: Dibujar el bitmap del mapa con cierta transparencia
                // Usar transparencia para que se vea la matriz debajo
                val alphaPaint = Paint().apply { alpha = 180 }  // 70% opaco
                canvas.drawBitmap(bitmap, 0f, 0f, alphaPaint)

                canvas.restore()

                // Paso 4: Dibujar jugadores (con transformaciones aplicadas nuevamente)
                canvas.save()
                canvas.translate(mapState.offsetX, mapState.offsetY)
                canvas.scale(mapState.scaleFactor, mapState.scaleFactor)

                // Dibujar jugadores
                playerManager.drawPlayers(canvas, mapState)

                canvas.restore()

                Log.d("MapRenderer", "Mapa dibujado con éxito: offset=(${mapState.offsetX},${mapState.offsetY}), scale=${mapState.scaleFactor}")
            } ?: run {
                drawErrorState(canvas)
            }
        } catch (e: Exception) {
            Log.e("MapRenderer", "Error dibujando mapa: ${e.message}")
            drawErrorState(canvas)
        }
    }

    // Métodos auxiliares para el dibujado de la matriz
    fun getPaintForCellType(cellType: Int): Paint {
        return when (cellType) {
            MapMatrixProvider.INTERACTIVE -> paintInteractive
            MapMatrixProvider.WALL -> paintWall
            MapMatrixProvider.PATH -> paintPath
            MapMatrixProvider.INACCESSIBLE -> paintInaccessible
            else -> paintPath
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