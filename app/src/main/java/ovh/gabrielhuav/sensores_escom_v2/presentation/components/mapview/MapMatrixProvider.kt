package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Provee matrices específicas para cada mapa del juego.
 * Cada mapa tiene su propia configuración de colisiones y puntos interactivos.
 */
class MapMatrixProvider {
    companion object {
        // Constantes compartidas para tipos de celdas
        const val INTERACTIVE = 0
        const val WALL = 1
        const val PATH = 2
        const val INACCESSIBLE = 3

        // Tamaño estándar de la matriz
        const val MAP_WIDTH = 40
        const val MAP_HEIGHT = 40

        // Mapas disponibles
        const val MAP_MAIN = "main"
        const val MAP_BUILDING2 = "escom_building2"

        // Puntos de transición entre mapas
        val MAIN_TO_BUILDING2_POSITION = Pair(15, 10)
        val BUILDING2_TO_MAIN_POSITION = Pair(1, 1)

        /**
         * Obtiene la matriz para el mapa especificado
         */
        fun getMatrixForMap(mapId: String): Array<Array<Int>> {
            return when (mapId) {
                MAP_MAIN -> createMainMapMatrix()
                MAP_BUILDING2 -> createBuilding2Matrix()
                else -> createDefaultMatrix() // Por defecto, un mapa básico
            }
        }

        /**
         * Matriz para el mapa principal del campus
         */
        private fun createMainMapMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Configuración de bordes
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                    // Zonas interactivas (edificios, entradas)
                    else if (i == 10 && j == 15) {
                        matrix[i][j] = INTERACTIVE // Entrada al edificio 2
                    }
                    // Obstáculos (árboles, bancas, etc)
                    else if (i % 7 == 0 && j % 8 == 0) {
                        matrix[i][j] = INACCESSIBLE
                    }
                    // Caminos especiales
                    else if ((i % 5 == 0 || j % 5 == 0) && i > 5 && j > 5) {
                        matrix[i][j] = PATH
                    }
                }
            }

            // Áreas de juego específicas
            // Zona central despejada
            for (i in 15..25) {
                for (j in 15..25) {
                    matrix[i][j] = PATH
                }
            }

            return matrix
        }

        /**
         * Matriz para el edificio 2
         */
        private fun createBuilding2Matrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Configuración de paredes interiores
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                    // Punto de salida al mapa principal
                    else if (i == 1 && j == 1) {
                        matrix[i][j] = INTERACTIVE
                    }
                    // Estructura de laboratorios (lado izquierdo)
                    else if (j > 3 && j < 18 && i > 3 && i < 18) {
                        // Paredes exteriores de los laboratorios
                        if (j == 4 || j == 17 || i == 4 || i == 17) {
                            matrix[i][j] = WALL
                        }
                        // Puertas a los laboratorios
                        else if ((j == 10 && i == 4) || (j == 10 && i == 17)) {
                            matrix[i][j] = PATH
                        }
                        // Divisiones internas
                        else if (j == 10 && i != 10) {
                            matrix[i][j] = WALL
                        }
                        // Pasillo central horizontal
                        else if (i == 10) {
                            matrix[i][j] = PATH
                        }
                        // Puntos de interés en los laboratorios
                        else if ((j == 7 && i == 7) || (j == 14 && i == 7) ||
                            (j == 7 && i == 14) || (j == 14 && i == 14)) {
                            matrix[i][j] = INTERACTIVE
                        }
                        // Objetos inamovibles (computadoras, mesas)
                        else if ((j == 6 && i == 6) || (j == 8 && i == 6) ||
                            (j == 13 && i == 6) || (j == 15 && i == 6) ||
                            (j == 6 && i == 13) || (j == 8 && i == 13) ||
                            (j == 13 && i == 13) || (j == 15 && i == 13)) {
                            matrix[i][j] = INACCESSIBLE
                        }
                    }

                    // Estructura de aulas (lado derecho)
                    else if (j > 22 && j < 37 && i > 3 && i < 37) {
                        // Paredes exteriores de las aulas
                        if (j == 23 || j == 36 || i == 4 || i == 36) {
                            matrix[i][j] = WALL
                        }
                        // Divisiones verticales entre aulas
                        else if (j == 30 && (i < 15 || i > 25)) {
                            matrix[i][j] = WALL
                        }
                        // Divisiones horizontales entre aulas
                        else if ((i == 15 || i == 25) && j > 22 && j < 37) {
                            matrix[i][j] = WALL
                        }
                        // Puertas a las aulas
                        else if ((j == 23 && i == 10) || (j == 30 && i == 20) ||
                            (j == 36 && i == 10) || (j == 36 && i == 30)) {
                            matrix[i][j] = PATH
                        }
                        // Puntos de interés en las aulas (pizarrones, proyectores)
                        else if ((j == 27 && i == 10) || (j == 33 && i == 10) ||
                            (j == 27 && i == 20) || (j == 33 && i == 20) ||
                            (j == 27 && i == 30) || (j == 33 && i == 30)) {
                            matrix[i][j] = INTERACTIVE
                        }
                        // Objetos inamovibles (escritorios, pupitres)
                        else if ((j == 25 && i == 8) || (j == 28 && i == 8) ||
                            (j == 32 && i == 8) || (j == 34 && i == 8) ||
                            (j == 25 && i == 18) || (j == 28 && i == 18) ||
                            (j == 32 && i == 18) || (j == 34 && i == 18) ||
                            (j == 25 && i == 28) || (j == 28 && i == 28) ||
                            (j == 32 && i == 28) || (j == 34 && i == 28)) {
                            matrix[i][j] = INACCESSIBLE
                        }
                    }

                    // Pasillos centrales
                    else if ((j == 20 && i > 4 && i < 36) || (i == 20 && j > 4 && j < 36)) {
                        matrix[i][j] = PATH
                    }

                    // Área de descanso central
                    else if (j > 17 && j < 23 && i > 17 && i < 23) {
                        if (j == 20 && i == 20) {
                            matrix[i][j] = INTERACTIVE  // Punto central interactivo
                        } else {
                            matrix[i][j] = PATH
                        }
                    }
                }
            }

            return matrix
        }

        /**
         * Matriz predeterminada para cualquier otro mapa
         */
        private fun createDefaultMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Borde simple
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            return matrix
        }

        /**
         * Comprueba si la coordenada especificada es un punto de transición entre mapas
         */
        fun isMapTransitionPoint(mapId: String, x: Int, y: Int): String? {
            return when {
                mapId == MAP_MAIN && x == MAIN_TO_BUILDING2_POSITION.first && y == MAIN_TO_BUILDING2_POSITION.second -> MAP_BUILDING2
                mapId == MAP_BUILDING2 && x == BUILDING2_TO_MAIN_POSITION.first && y == BUILDING2_TO_MAIN_POSITION.second -> MAP_MAIN
                else -> null
            }
        }

        /**
         * Obtiene la posición inicial para un mapa destino
         */
        fun getInitialPositionForMap(mapId: String): Pair<Int, Int> {
            return when (mapId) {
                MAP_MAIN -> BUILDING2_TO_MAIN_POSITION
                MAP_BUILDING2 -> MAIN_TO_BUILDING2_POSITION
                else -> Pair(MAP_WIDTH / 2, MAP_HEIGHT / 2)
            }
        }
    }
}

/**
 * Gestor de matriz para un mapa específico
 */
class MapMatrix(private val mapId: String) {
    private val matrix: Array<Array<Int>> = MapMatrixProvider.getMatrixForMap(mapId)

    private val paints = mapOf(
        MapMatrixProvider.INTERACTIVE to Paint().apply {
            color = Color.rgb(0, 255, 255)  // Cian brillante para puntos interactivos
            alpha = 200
        },
        MapMatrixProvider.WALL to Paint().apply {
            color = Color.rgb(139, 69, 19)  // Marrón (simular madera) para paredes
        },
        MapMatrixProvider.PATH to Paint().apply {
            color = Color.rgb(220, 220, 255)  // Gris azulado claro para caminos
        },
        MapMatrixProvider.INACCESSIBLE to Paint().apply {
            color = Color.rgb(178, 34, 34)  // Rojo ladrillo para objetos inamovibles
            alpha = 180
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
        val cellWidth = width / MapMatrixProvider.MAP_WIDTH
        val cellHeight = height / MapMatrixProvider.MAP_HEIGHT

        for (y in 0 until MapMatrixProvider.MAP_HEIGHT) {
            for (x in 0 until MapMatrixProvider.MAP_WIDTH) {
                val paint = paints[matrix[y][x]] ?: paints[MapMatrixProvider.PATH]!!
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
}