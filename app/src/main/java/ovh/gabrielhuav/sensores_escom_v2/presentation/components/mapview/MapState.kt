package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.R

class MapState {
    var backgroundBitmap: Bitmap? = null
    var offsetX = 0f
    var offsetY = 0f
    var scaleFactor = 1f

    val mapMatrix = Array(40) { Array(40) { 2 } }.apply {
        for (i in 0 until 40) {
            for (j in 0 until 40) {
                this[i][j] = when {
                    i == 0 || i == 39 || j == 0 || j == 39 -> 1 // Bordes
                    i % 5 == 0 && j % 5 == 0 -> 0 // Lugares interactivos
                    i % 3 == 0 && j % 3 == 0 -> 3 // Zonas inaccesibles
                    else -> 2 // Camino libre
                }
            }
        }
    }

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
        val matrixAspectRatio = mapMatrix[0].size.toFloat() / mapMatrix.size
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
}
