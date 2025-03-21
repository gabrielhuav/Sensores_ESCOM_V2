package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.util.Log

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

        // Constantes para los mapas
        const val MAP_MAIN = "escom_main"
        const val MAP_BUILDING2 = "escom_building2"
        const val MAP_BUILDING4_F2 = "escom_building4_floor_2"
        const val MAP_SALON2009 = "escom_salon2009"
        const val MAP_SALON2010 = "escom_salon2010"
        const val MAP_CAFETERIA = "escom_cafeteria"
<<<<<<< HEAD
        const val MAP_ESTACIONAMIENTO = "escom_estacionamiento"
        const val MAP_PALAPAS = "escom_palapas"
=======
        const val MAP_SALON1212 = "escom_salon1212"
>>>>>>> Palapas_IA/main


        fun normalizeMapName(mapName: String?): String {
            if (mapName.isNullOrBlank()) return MAP_MAIN

            val lowerMap = mapName.lowercase()

            return when {
                // Mapa principal
                lowerMap == "main" -> MAP_MAIN
                lowerMap == "map_main" -> MAP_MAIN
                lowerMap.contains("main") && !lowerMap.contains("building") -> MAP_MAIN

                // Edificio 2
                lowerMap.contains("building2") || lowerMap.contains("edificio2") -> MAP_BUILDING2

                // Salones
                lowerMap.contains("2009") || lowerMap.contains("salon2009") -> MAP_SALON2009
                lowerMap.contains("2010") || lowerMap.contains("salon2010") -> MAP_SALON2010
                lowerMap.contains("1212") || lowerMap.contains("salon1212") -> MAP_SALON1212

                // Cafetería
                lowerMap.contains("cafe") || lowerMap.contains("cafeteria") -> MAP_CAFETERIA

                // Estacionamiento
                lowerMap.contains("estacionamiento") || lowerMap.contains("estacionamiento") -> MAP_ESTACIONAMIENTO

                // Palapas
                lowerMap.contains("palapas IA") || lowerMap.contains("palapas IA") -> MAP_PALAPAS

                // Si no coincide con ninguno de los anteriores, devolver el original
                else -> mapName
            }
        }

<<<<<<< HEAD
        // Puntos de transición entre mapas
        val MAIN_TO_BUILDING2_POSITION = Pair(15, 10)
        val BUILDING2_TO_MAIN_POSITION = Pair(5, 5)  // Posición segura en la esquina superior izquierda
        val BUILDING2_TO_SALON2009_POSITION = Pair(15, 16)  // Punto en el pasillo principal
        val SALON2009_TO_BUILDING2_POSITION = Pair(1, 20)  // Punto en la puerta del salón

        val BUILDING2_TO_SALON2010_POSITION = Pair(20, 20)  // Desde edificio 2
        val MAIN_TO_SALON2010_POSITION = Pair(25, 25)       // Desde mapa principal
        val SALON2010_TO_BUILDING2_POSITION = Pair(5, 5)    // Vuelta al edificio 2
        val SALON2010_TO_MAIN_POSITION = Pair(1, 1)         // Vuelta al mapa principal

        val MAIN_TO_CAFETERIA_POSITION = Pair(2, 2)       // Desde mapa principal
        val CAFETERIA_TO_MAIN_POSITION = Pair(1, 1)         // Vuelta al mapa principal

        val MAIN_TO_ESTACIONAMIENTO_POSITION = Pair(2, 2)       // Desde mapa principal
        val ESTACIONAMIENTO_TO_MAIN_POSITION = Pair(1, 1)       // Vuelta al mapa principal

        val MAIN_TO_PALAPAS_POSITION = Pair(2, 2)       // Desde mapa principal
        val PALAPAS_TO_MAIN_POSITION = Pair(1, 1)       // Vuelta al mapa principal

=======
>>>>>>> Palapas_IA/main
        /**
         * Obtiene la matriz para el mapa especificado
         */
        fun getMatrixForMap(mapId: String): Array<Array<Int>> {
            return when (mapId) {
                MAP_MAIN -> createMainMapMatrix()
                MAP_BUILDING2 -> createBuilding2Matrix()
                MAP_BUILDING4_F2 -> createBuilding2Matrix()
                MAP_SALON2009 -> createSalon2009Matrix()  // Nueva matriz para el salón 2009
                MAP_SALON2010 -> createSalon2010Matrix()  // Nueva matriz para el salón 2010
                MAP_SALON1212 -> createSalon1212Matrix()
                MAP_CAFETERIA -> createCafeESCOMMatrix()
                MAP_ESTACIONAMIENTO -> createEstacionamientoMatrix()
                MAP_PALAPAS -> createPalapasMatrix()
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
                    // Zonas interactivas (edificios, entradas)
                    else if (i == 10 && j == 23) {
                        matrix[i][j] = INTERACTIVE // Entrada al edificio 4
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
            // Explicitly set coordinates 29,22 and 29,23 as blue interactive points
            matrix[28][27] = INTERACTIVE

            // Áreas de juego específicas
            // Zona central despejada
            for (i in 15..25) {
                for (j in 15..25) {
                    matrix[i][j] = PATH
                }
            }

            return matrix
        }

        fun createSalon1212Matrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } }

            // Make the classroom interior mostly navigable
            for (i in 5 until MAP_HEIGHT - 5) {
                for (j in 5 until MAP_WIDTH - 5) {
                    matrix[i][j] = PATH
                }
            }

            // Add some obstacles for the Pacman game (tables, chairs, etc.)
            // Row of tables in the center
            for (i in 13 until 17) {
                for (j in 10 until 30) {
                    if (j % 5 < 3) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // Teacher's desk at the front
            for (i in 6 until 9) {
                for (j in 15 until 25) {
                    matrix[i][j] = WALL
                }
            }

            // Back row computers
            for (j in 8 until MAP_WIDTH - 8) {
                matrix[MAP_HEIGHT - 8][j] = WALL
            }

            // Door/exit point (interaction point to return to building)
            matrix[MAP_HEIGHT - 6][5] = INTERACTIVE
            matrix[MAP_HEIGHT - 6][6] = INTERACTIVE

            return matrix
        }

        /**
         * Matriz para el edificio 2
         * Basada exactamente en el mapa ASCII:
         * +-------------------------------------------------------------------------+
         * |                               Edificio 2                                |
         * |                              Planta Baja                                |
         * |                                                                         |
         * |  +--------+--------+--------+-----+--------+--------+--------+----+     |
         * |  |  2001  |  2002  |  2003  | ⬆️  |  2004  |  2005  |  2006  | 🚾 |     |
         * |  |🏫 Aula |🏫 Aula |🏫 Aula | 🪜  |🏫 Aula |🏫 Aula |🏫 Aula | WC |     |
         * |  +🚪------+🚪------+🚪------+ ⬇️  +🚪------+🚪------+🚪------+🚪--+     |
         * |                                                                         |
         * |                      [    Pasillo Principal 🚶    ]                     |
         * |                                                                         |
         * +-------------------------------------------------------------------------+
         */
        private fun createBuilding2Matrix(): Array<Array<Int>> {
            // Start with everything as PATH (2) to make most areas walkable
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Add outer walls
            for (i in 0 until MAP_HEIGHT) {
                matrix[i][0] = WALL
                matrix[i][MAP_WIDTH - 1] = WALL
            }
            for (j in 0 until MAP_WIDTH) {
                matrix[0][j] = WALL
                matrix[MAP_HEIGHT - 1][j] = WALL
            }

            // Based on ASCII art:
            // +-------------------------------------------------------------------------+
            // |                               Edificio 2                                |
            // |                              Planta Baja                                |
            // |                                                                         |
            // |  +--------+--------+--------+-----+--------+--------+--------+----+     |
            // |  |  2001  |  2002  |  2003  | ⬆️  |  2004  |  2005  |  2006  | 🚾 |     |
            // |  |🏫 Aula |🏫 Aula |🏫 Aula | 🪜  |🏫 Aula |🏫 Aula |🏫 Aula | WC |     |
            // |  +🚪------+🚪------+🚪------+ ⬇️  +🚪------+🚪------+🚪------+🚪--+     |
            // |                                                                         |
            // |                      [    Pasillo Principal 🚶    ]                     |
            // |                                                                         |
            // +-------------------------------------------------------------------------+

            // Define the classroom row
            val roomTop = 10
            val roomHeight = 10
            val corridorY = roomTop + roomHeight
            val roomWidth = 5

            // Draw the top walls of classrooms
            for (x in 5 until 40 - 5) {
                matrix[roomTop][x] = WALL
            }

            // Draw the dividing walls between classrooms
            // We'll have 7 classrooms total
            for (roomNum in 0..7) {
                val wallX = 5 + (roomNum * roomWidth)
                if (wallX < MAP_WIDTH - 5) {
                    for (y in roomTop until roomTop + roomHeight) {
                        matrix[y][wallX] = WALL
                    }
                }
            }

            // Bottom wall of classrooms (top of corridor)
            for (x in 5 until 40 - 5) {
                matrix[corridorY][x] = WALL
            }

            // Add doors to classrooms
            for (roomNum in 0..6) {
                // Skip room 3 which is stairs
                if (roomNum != 3) {
                    val doorX = 5 + (roomNum * roomWidth) + 2
                    matrix[corridorY][doorX] = PATH

                    // Make salon 2006 (room 6) door interactive
                    if (roomNum == 5) {
                        matrix[corridorY][doorX - 1] = INTERACTIVE
                        matrix[corridorY][doorX] = INTERACTIVE
                        matrix[corridorY][doorX + 1] = INTERACTIVE
                    }
                }
            }

            // Make stairs area in room 3
            val stairsX = 5 + (3 * roomWidth) + 2
            for (y in roomTop + 2 until corridorY) {
                matrix[y][stairsX] = INTERACTIVE
            }

            // Mark corridor area
            // The corridor is below the classrooms
            for (y in corridorY + 1 until corridorY + 4) {
                for (x in 5 until 40 - 5) {
                    matrix[y][x] = PATH
                }
            }

            // Bottom wall of corridor
            for (x in 5 until 40 - 5) {
                matrix[corridorY + 4][x] = WALL
            }

            // Exit point from building
            matrix[corridorY + 2][5] = INTERACTIVE

            // Key interactive points (salon 2006 entrance)
            // Explicitly set coordinates 29,22 and 29,23 as blue interactive points
            matrix[22][29] = INTERACTIVE
            matrix[23][29] = INTERACTIVE

            matrix[22][24] = INTERACTIVE
            matrix[23][24] = INTERACTIVE

            // Add labels to help with debugging
            Log.d("MapMatrix", "Interactive value = $INTERACTIVE")
            Log.d("MapMatrix", "Wall value = $WALL")
            Log.d("MapMatrix", "Path value = $PATH")
            Log.d("MapMatrix", "Value at (29, 22): ${matrix[22][29]}")
            Log.d("MapMatrix", "Value at (29, 23): ${matrix[23][29]}")

            return matrix
        }
<<<<<<< HEAD

=======
>>>>>>> Palapas_IA/main
        /**
         * Matriz para el salón 2009
         */
        private fun createSalon2009Matrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } }

            // Dimensiones del aula
            val roomWidth = 30
            val roomHeight = 25
            val startX = 5
            val startY = 5

            // Interior del salón (espacio abierto)
            for (i in startY until startY + roomHeight) {
                for (j in startX until startX + roomWidth) {
                    matrix[i][j] = PATH
                }
            }

            // Puerta de salida hacia el edificio 2 (lado izquierdo)
            matrix[startY + roomHeight/2][1] = INTERACTIVE

            // Pizarrón (pared frontal)
            for (j in startX + 2 until startX + roomWidth - 2) {
                matrix[startY + 1][j] = INACCESSIBLE
            }
            // Centro del pizarrón es interactivo
            matrix[startY + 1][startX + roomWidth/2] = INTERACTIVE

            // Escritorio del profesor
            for (j in startX + 10 until startX + 20) {
                for (i in startY + 3 until startY + 6) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            // Filas de pupitres para estudiantes
            for (row in 0 until 4) {
                val rowY = startY + 8 + (row * 4)

                // 5 pupitres por fila
                for (desk in 0 until 5) {
                    val deskX = startX + 3 + (desk * 5)

                    // Cada pupitre ocupa 3x2
                    for (i in rowY until rowY + 2) {
                        for (j in deskX until deskX + 3) {
                            matrix[i][j] = INACCESSIBLE
                        }
                    }
                }
            }

            return matrix
        }

        private fun createSalon2010Matrix(): Array<Array<Int>> {
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
         * Matriz para el mapa principal del campus
         */
        private fun createCafeESCOMMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Definición de constantes para mejorar legibilidad
            val PARED = WALL
            val CAMINO = PATH
            val BANCA = INACCESSIBLE
            val INTERACTIVO = INTERACTIVE

            // Bordes exteriores - paredes del restaurante
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = PARED
                    }
                }
            }

            // COCINA (esquina superior izquierda)
            for (i in 2..8) {
                for (j in 2..15) {
                    if (i == 2 || i == 8 || j == 2 || j == 15) {
                        matrix[i][j] = PARED // Paredes de la cocina
                    }
                }
            }
            // Mostrador de la cocina
            for (i in 4..6) {
                for (j in 4..13) {
                    matrix[i][j] = BANCA
                }
            }

            // MESAS/BANCAS LARGAS (3 filas de 3 mesas cada una)
            // Primera fila de mesas
            for (row in 0..2) {
                for (col in 0..2) {
                    // Cada mesa es un rectángulo
                    val baseI = 12 + (row * 8)
                    val baseJ = 10 + (col * 10)

                    for (i in baseI..baseI+2) {
                        for (j in baseJ..baseJ+8) {
                            matrix[i][j] = BANCA
                        }
                    }
                }
            }

            // CAJA (parte inferior)
            for (i in 30..33) {
                for (j in 15..19) {
                    matrix[i][j] = BANCA
                }
            }

            // ENTRADA
            for (i in 37..38) {
                for (j in 15..25) {
                    matrix[i][j] = INTERACTIVO
                }
            }

            // Agregar elementos interactivos: Tacos, Burritos, Guacamole y Chile
            // Tacos (representados como puntos interactivos)
            matrix[12][8] = INTERACTIVO
            matrix[12][32] = INTERACTIVO
            matrix[28][8] = INTERACTIVO
            matrix[28][32] = INTERACTIVO

            // Burritos
            matrix[12][33] = INTERACTIVO
            matrix[28][33] = INTERACTIVO

            // Guacamole
            matrix[20][8] = INTERACTIVO

            // Chile
            matrix[20][32] = INTERACTIVO

            return matrix
        }

        /**
         * Matriz para el mapa principal del campus
         */
        private fun createEstacionamientoMatrix(): Array<Array<Int>> {
            val WALL = -1                // 🟫 Pared/Borde del mapa
            val PATH = 0                 // 🟩 Espacio transitable
            val PARKING_SPOT = 1         // 🚗 Espacio de estacionamiento (autos)
            val MOTORCYCLE_PARKING = 2   // 🏍️ Espacio de estacionamiento (motos)
            val ROAD = 3                 // 🛣️ Calle interna del estacionamiento
            val TREE = 4                 // 🌳 Obstáculo (árboles o bancas)
            val INTERACTIVE = 5          // 🏫 Zona de interacción (accesos a edificios)

            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Configuración de bordes (paredes exteriores)
            for (i in 0 until MAP_HEIGHT) {
                matrix[i][0] = WALL  // Lado izquierdo
                matrix[i][MAP_WIDTH - 1] = WALL  // Lado derecho
            }
            for (j in 0 until MAP_WIDTH) {
                matrix[0][j] = WALL  // Parte superior
                matrix[MAP_HEIGHT - 1][j] = WALL  // Parte inferior
            }

            // 🚗 **Zonas de estacionamiento en las orillas** (cajones de autos)
            for (i in 2 until MAP_HEIGHT step 6) {
                for (j in 2 until MAP_WIDTH step 10) {
                    matrix[i][j] = PARKING_SPOT
                    matrix[i][j + 1] = PARKING_SPOT
                }
            }

            // 🏍️ **Zona de estacionamiento para motos**
            for (i in 5 until MAP_HEIGHT step 10) {
                for (j in 8 until 12) {
                    matrix[i][j] = MOTORCYCLE_PARKING
                }
            }

            // 🚶 **Calles en forma de laberinto** (zonas transitables)
            for (i in 3 until MAP_HEIGHT step 4) {
                for (j in 1 until MAP_WIDTH step 5) {
                    if (matrix[i][j] == PATH) {
                        matrix[i][j] = ROAD
                    }
                }
            }

            // 🌳 **Distribución de árboles y bancas** en los espacios de descanso
            for (i in 6 until MAP_HEIGHT step 9) {
                for (j in 5 until MAP_WIDTH step 7) {
                    if (matrix[i][j] == PATH) {
                        matrix[i][j] = TREE
                    }
                }
            }

            // 🏫 **Zonas interactivas (edificios y accesos)**
            matrix[10][15] = INTERACTIVE  // Entrada a otro edificio
            matrix[MAP_HEIGHT / 2][MAP_WIDTH / 2] = INTERACTIVE  // Punto central de acceso

            // 🔲 **Zona central despejada** (para maniobras o circulación)
            for (i in MAP_HEIGHT / 2 - 3..MAP_HEIGHT / 2 + 3) {
                for (j in MAP_WIDTH / 2 - 3..MAP_WIDTH / 2 + 3) {
                    matrix[i][j] = PATH
                }
            }
            return matrix
        }

        /**
         * Matriz para el mapa principal del campus
         */
        private fun createPalapasMatrix(): Array<Array<Int>> {
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
            // Imprimimos para depuración
            Log.d("MapTransition", "Checking transition at $mapId: ($x, $y)")

            // Para el edificio 2, cualquier punto interactivo cerca del centro del pasillo
            // nos lleva al salón 2009
            if (mapId == MAP_BUILDING2) {
                // Si estamos en o cerca de las coordenadas (15,16) o cualquiera de las alternativas
                //val nearCenter = (x >= 14 && x <= 16 && y >= 15 && y <= 17)
                val alternative1 = (x == 29 && y == 23)
                val alternative2 = (x == 29 && y == 22)

                if (alternative1 || alternative2) {
                    Log.d("MapTransition", "Transition to salon2009 triggered!")
                    return MAP_SALON2009
                }

                if (x == 24 && y == 22 || x == 24 && y == 23 ) {
                    return MAP_SALON2010
                }

                // Punto para regresar al mapa principal
                if (x == 5 && y == 5) {
                    return MAP_MAIN
                }
            }

            // Si estamos en el salón 2009, la coordenada (1,20) nos lleva de vuelta al edificio 2
            if (mapId == MAP_SALON2009 && x == 1 && y == 20) {
                return MAP_BUILDING2
            }

            if (mapId == MAP_SALON2010) {
                if (x == 5 && y == 5) {
                    return MAP_BUILDING2
                }
                if (x == 10 && y == 10) {
                    return MAP_MAIN
                }
            }

            if (mapId == MAP_MAIN && x == 33 && y == 34) {
                return MAP_CAFETERIA
            }

<<<<<<< HEAD
            if (mapId == MAP_MAIN && x == 9 && y == 5) {
                return MAP_ESTACIONAMIENTO
            }

            if (mapId == MAP_MAIN && x == 31 && y == 11) {
                return MAP_PALAPAS
            }

=======
            if (mapId == MAP_MAIN && x == 23 && y == 10) {
                return MAP_BUILDING4_F2
            }

            // Add this case for the main map to salon1212
            if (mapId == MAP_MAIN && x == 27 && y == 28) {
                return MAP_SALON1212
            }

            // Add this case for returning from salon1212 to building
            if (mapId == MAP_SALON1212 && (x == 5 || x == 6) && y == MAP_HEIGHT - 6) {
                return MAP_BUILDING2
            }


>>>>>>> Palapas_IA/main
            // Resto de transiciones...

            return null
        }


        /**
         * Obtiene la posición inicial para un mapa destino
         */
        fun getInitialPositionForMap(mapId: String): Pair<Int, Int> {
            return when (mapId) {
                MAP_MAIN -> Pair(15, 15)  // Posición central en el mapa principal
                MAP_BUILDING4_F2 -> Pair(20, 16)  // Centro del pasillo principal del edificio 4
                MAP_BUILDING2 -> Pair(20, 16)  // Centro del pasillo principal del edificio 2
                MAP_SALON2009 -> Pair(20, 20)  // Posición central dentro del salón 2009
                MAP_SALON2010 -> Pair(20, 20)  // Posición central dentro del salón 2010
                MAP_CAFETERIA -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_ESTACIONAMIENTO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                else -> Pair(MAP_WIDTH / 2, MAP_HEIGHT / 2)
            }
        }
    }
}