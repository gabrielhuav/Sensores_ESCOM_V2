package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.R

class MapState {
    var backgroundBitmap: Bitmap? = null
    var offsetX = 0f
    var offsetY = 0f
    var scaleFactor = 1f

    // Matriz del mapa actual (se inicializa con una matriz vacía)
    private var currentMapMatrix: MapMatrix? = null

    // Actualizamos para usar la nueva implementación de MapMatrix
    var mapMatrix: Array<Array<Int>> = Array(MapMatrixProvider.MAP_HEIGHT) { Array(MapMatrixProvider.MAP_WIDTH) { MapMatrixProvider.PATH } }

    fun onSizeChanged(w: Int, h: Int, context: Context) {
        try {
            val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.escom_mapa)
            if (originalBitmap != null) {
                // Limitar el tamaño del Bitmap al tamaño de la vista
                val scaledWidth = minOf(originalBitmap.width, w)
                val scaledHeight = minOf(originalBitmap.height, h)

                backgroundBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                originalBitmap.recycle() // Liberar el bitmap original
                scaleBitmapToMatrix()
            } else {
                Log.e("MapState", "Error: Mapa no encontrado en los recursos.")
            }
        } catch (e: Exception) {
            Log.e("MapState", "Error al cargar el mapa: ${e.message}")
        }
    }

    private fun scaleBitmapToMatrix() {
        if (backgroundBitmap == null) return

        val maxBitmapSize = 2048 // Tamaño máximo permitido
        val matrixWidth = MapMatrixProvider.MAP_WIDTH
        val matrixHeight = MapMatrixProvider.MAP_HEIGHT
        val matrixAspectRatio = matrixWidth.toFloat() / matrixHeight
        val bitmapAspectRatio = backgroundBitmap!!.width.toFloat() / backgroundBitmap!!.height

        val scaledWidth: Int
        val scaledHeight: Int

        if (matrixAspectRatio > bitmapAspectRatio) {
            scaledWidth = (backgroundBitmap!!.height * matrixAspectRatio).toInt().coerceAtMost(maxBitmapSize)
            scaledHeight = backgroundBitmap!!.height.coerceAtMost(maxBitmapSize)
        } else {
            scaledWidth = backgroundBitmap!!.width.coerceAtMost(maxBitmapSize)
            scaledHeight = (backgroundBitmap!!.width / matrixAspectRatio).toInt().coerceAtMost(maxBitmapSize)
        }

        val oldBitmap = backgroundBitmap
        backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap!!, scaledWidth, scaledHeight, true)
        if (oldBitmap != backgroundBitmap) {
            oldBitmap?.recycle()
        }
    }

    fun constrainOffset(viewWidth: Int, viewHeight: Int) {
        if (backgroundBitmap == null) return

        val scaledWidth = backgroundBitmap!!.width * scaleFactor
        val scaledHeight = backgroundBitmap!!.height * scaleFactor

        val maxOffsetX = -(scaledWidth - viewWidth)
        val maxOffsetY = -(scaledHeight - viewHeight)

        offsetX = offsetX.coerceIn(maxOffsetX, 0f)
        offsetY = offsetY.coerceIn(maxOffsetY, 0f)
    }

    // Método para establecer la matriz del mapa actual
    fun setMapMatrix(mapMatrix: MapMatrix) {
        currentMapMatrix = mapMatrix
    }

    // Método para dibujar la matriz actual
    fun drawMapMatrix(canvas: Canvas, width: Float, height: Float) {
        try {
            currentMapMatrix?.drawMatrix(canvas, width, height) ?: run {
                // Si no hay matriz, dibujar una matriz por defecto
                drawDefaultMatrix(canvas, width, height)
            }
        } catch (e: Exception) {
            Log.e("MapState", "Error dibujando matriz: ${e.message}")
        }
    }

    // Matriz por defecto si no hay una matriz específica
    private fun drawDefaultMatrix(canvas: Canvas, width: Float, height: Float) {
        val cellWidth = width / MapMatrixProvider.MAP_WIDTH
        val cellHeight = height / MapMatrixProvider.MAP_HEIGHT

        for (y in 0 until MapMatrixProvider.MAP_HEIGHT) {
            for (x in 0 until MapMatrixProvider.MAP_WIDTH) {
                val paint = when {
                    x == 0 || y == 0 || x == MapMatrixProvider.MAP_WIDTH - 1 || y == MapMatrixProvider.MAP_HEIGHT - 1 -> Paint().apply { color = Color.BLACK }
                    else -> Paint().apply { color = Color.WHITE }
                }
                canvas.drawRect(
                    x * cellWidth,
                    y * cellHeight,
                    (x + 1) * cellWidth,
                    (y + 1) * cellHeight,
                    paint
                )
            }
        }
    }
}