package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log

/**
 * Gestor de matriz para un mapa específico
 */
class MapMatrix(private val mapId: String) {
    private val matrix: Array<Array<Int>> = MapMatrixProvider.getMatrixForMap(mapId)

    private val paints = mapOf(
        MapMatrixProvider.INTERACTIVE to Paint().apply {
            color = Color.argb(100, 0, 255, 255)  // Cian semi-transparente para puntos interactivos
        },
        MapMatrixProvider.WALL to Paint().apply {
            color = Color.argb(150, 139, 69, 19)  // Marrón semi-transparente para paredes
        },
        MapMatrixProvider.PATH to Paint().apply {
            color = Color.argb(30, 220, 220, 255)  // Gris azulado muy transparente para caminos
        },
        MapMatrixProvider.INACCESSIBLE to Paint().apply {
            color = Color.argb(120, 178, 34, 34)  // Rojo ladrillo semi-transparente para objetos
        }
    )

    fun getValueAt(x: Int, y: Int): Int {
        return if (x in 0 until MapMatrixProvider.MAP_WIDTH && y in 0 until MapMatrixProvider.MAP_HEIGHT) {
            matrix[y][x]
        } else {
            -1
        }
    }

    fun isValidPosition(x: Int, y: Int): Boolean {
        return x in 0 until MapMatrixProvider.MAP_WIDTH &&
                y in 0 until MapMatrixProvider.MAP_HEIGHT &&
                matrix[y][x] != MapMatrixProvider.WALL &&
                matrix[y][x] != MapMatrixProvider.INACCESSIBLE
    }

    fun isInteractivePosition(x: Int, y: Int): Boolean {
        return x in 0 until MapMatrixProvider.MAP_WIDTH &&
                y in 0 until MapMatrixProvider.MAP_HEIGHT &&
                matrix[y][x] == MapMatrixProvider.INTERACTIVE
    }

    fun isMapTransitionPoint(x: Int, y: Int): String? {
        return MapMatrixProvider.isMapTransitionPoint(mapId, x, y)
    }

    fun drawMatrix(canvas: Canvas, width: Float, height: Float) {
        try {
            val cellWidth = width / MapMatrixProvider.MAP_WIDTH
            val cellHeight = height / MapMatrixProvider.MAP_HEIGHT

            // Usar distintas opacidades para que el mapa se vea bien
            for (y in 0 until MapMatrixProvider.MAP_HEIGHT) {
                for (x in 0 until MapMatrixProvider.MAP_WIDTH) {
                    val cellType = matrix[y][x]
                    val paint = paints[cellType] ?: paints[MapMatrixProvider.PATH]!!

                    // Calcular posición exacta de la celda
                    val left = x * cellWidth
                    val top = y * cellHeight
                    val right = left + cellWidth
                    val bottom = top + cellHeight

                    // Dibujar la celda
                    canvas.drawRect(left, top, right, bottom, paint)
                }
            }

            // Opcional: Dibujar un borde alrededor de todo el mapa para delimitarlo
            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(0f, 0f, width, height, borderPaint)
        } catch (e: Exception) {
            Log.e("MapMatrix", "Error dibujando matriz: ${e.message}")
        }
    }
}