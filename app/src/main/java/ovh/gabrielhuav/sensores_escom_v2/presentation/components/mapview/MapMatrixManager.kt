package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class MapMatrixManager {
    // Matriz del mapa
    private val matrix = Array(40) { Array(40) { 2 } }.apply {
        for (i in 0 until 40) {
            for (j in 0 until 40) {
                this[i][j] = when {
                    i == 0 || i == 39 || j == 0 || j == 39 -> WALL
                    i % 5 == 0 && j % 5 == 0 -> INTERACTIVE
                    i % 3 == 0 && j % 3 == 0 -> INACCESSIBLE
                    else -> PATH
                }
            }
        }
    }

    private val paints = mapOf(
        INTERACTIVE to Paint().apply {
            color = Color.YELLOW
            alpha = 180
        },
        WALL to Paint().apply {
            color = Color.BLACK
        },
        PATH to Paint().apply {
            color = Color.WHITE
        },
        INACCESSIBLE to Paint().apply {
            color = Color.DKGRAY
            alpha = 180
        }
    )

    fun getValueAt(x: Int, y: Int): Int {
        return if (x in 0 until 40 && y in 0 until 40) {
            matrix[y][x]
        } else {
            -1
        }
    }

    fun isValidPosition(x: Int, y: Int): Boolean {
        return x in 0 until 40 &&
                y in 0 until 40 &&
                matrix[y][x] != WALL &&
                matrix[y][x] != INACCESSIBLE
    }

    fun isInteractivePosition(x: Int, y: Int): Boolean {
        return x in 0 until 40 &&
                y in 0 until 40 &&
                matrix[y][x] == INTERACTIVE
    }

    fun drawMatrix(canvas: Canvas, width: Float, height: Float) {
        val cellWidth = width / 40f
        val cellHeight = height / 40f

        for (y in 0 until 40) {
            for (x in 0 until 40) {
                val paint = paints[matrix[y][x]] ?: paints[PATH]!!
                canvas.drawRect(
                    x * cellWidth,    // left
                    y * cellHeight,   // top
                    (x + 1) * cellWidth,  // right
                    (y + 1) * cellHeight, // bottom
                    paint
                )
            }
        }
    }

    companion object {
        const val INTERACTIVE = 0
        const val WALL = 1
        const val PATH = 2
        const val INACCESSIBLE = 3
    }
}
