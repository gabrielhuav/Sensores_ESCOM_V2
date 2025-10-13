package ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview

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
        const val MAP_BUILDING2_PISO1 = "escom_building2_piso1"
        const val MAP_BUILDING4_F2 = "escom_building4_floor_2"
        const val MAP_SALON2009 = "escom_salon2009"
        const val MAP_SALON2010 = "escom_salon2010"
        const val MAP_CAFETERIA = "escom_cafeteria"
        const val MAP_SALON1212 = "escom_salon1212"
        const val MAP_ZACATENCO = "escom_zacatenco"
        const val MAP_LINDAVISTA = "escom_lindavista"
        const val MAP_ESTACIONAMIENTO = "EstacionamientoEscom"
        const val MAP_TRAS_PLAZA = "TramoAtrasPlaza"
        const val MAP_EDIFICIO_IA_BAJO = "edificio_ia_bajo"
        const val MAP_EDIFICIO_IA_MEDIO = "edificio_ia_medio"
        const val MAP_EDIFICIO_IA_ALTO = "edificio_ia_alto"
        const val MAP_CABLEBUS = "cablebus"
        const val MAP_SALIDAMETRO = "escom_salidametro"
        const val MAP_PALAPAS_IA = "escom_palapas_ia"
        const val MAP_PALAPAS_ISC = "escom_palapas_isc"
        const val MAP_EDIFICIO_GOBIERNO = "escom_edificio_gobierno"
        const val MAP_BIBLIOTECA = "escom_biblioteca"
        const val MAP_ENCB = "encb"

        const val MAP_ESIA = "esia"


        fun normalizeMapName(mapName: String?): String {
            if (mapName.isNullOrBlank()) return MAP_MAIN

            val lowerMap = mapName.lowercase()

            return when {
                // Mapa principal
                lowerMap == "main" -> MAP_MAIN
                lowerMap == "map_main" -> MAP_MAIN
                lowerMap.contains("main") && !lowerMap.contains("building") -> MAP_MAIN

                // Edificio 2
                lowerMap.contains("escom_building2_piso1") -> MAP_BUILDING2_PISO1
                lowerMap.contains("building2") || lowerMap.contains("edificio2") -> MAP_BUILDING2

                // Salones
                lowerMap.contains("2009") || lowerMap.contains("salon2009") -> MAP_SALON2009
                lowerMap.contains("2010") || lowerMap.contains("salon2010") -> MAP_SALON2010
                lowerMap.contains("1212") || lowerMap.contains("salon1212") -> MAP_SALON1212

                // Cafetería
                lowerMap.contains("cafe") || lowerMap.contains("cafeteria") -> MAP_CAFETERIA

                lowerMap.contains("estacionamiento") -> MAP_ESTACIONAMIENTO
                lowerMap.contains("plaza") || lowerMap.contains("atras") -> MAP_TRAS_PLAZA

                // Zacatenco
                lowerMap.contains("zaca") || lowerMap.contains("zacatenco") -> MAP_ZACATENCO
                // Lindavista
                lowerMap.contains("linda") || lowerMap.contains("lindavista") -> MAP_LINDAVISTA
                // edificio ia
                lowerMap.contains("ia_baja") || lowerMap.contains("edificio_ia_bajo") -> MAP_EDIFICIO_IA_BAJO
                lowerMap.contains("ia_medio") || lowerMap.contains("edificio_ia_medio") -> MAP_EDIFICIO_IA_MEDIO
                lowerMap.contains("ia_alto") || lowerMap.contains("edificio_ia_alto") -> MAP_EDIFICIO_IA_ALTO
                lowerMap.contains("cable") || lowerMap.contains("cablebus") -> MAP_CABLEBUS
                lowerMap.contains("palapas_ia") -> MAP_PALAPAS_IA

                lowerMap.contains("gobierno") || lowerMap.contains("edificio_gobierno") -> MAP_EDIFICIO_GOBIERNO
                lowerMap.contains("biblioteca") -> MAP_BIBLIOTECA
                // ESIA
                lowerMap.contains("esia") -> MAP_ESIA
                lowerMap.contains("encb") -> MAP_ENCB
                // Si no coincide con ninguno de los anteriores, devolver el original
                else -> mapName
            }
        }
        // Puntos de transición entre mapas existentes
        val MAIN_TO_BUILDING2_POSITION = Pair(15, 10)
        val BUILDING2_TO_MAIN_POSITION = Pair(5, 5)
        val BUILDING2_TO_SALON2009_POSITION = Pair(15, 16)
        val SALON2009_TO_BUILDING2_POSITION = Pair(1, 20)
        val BUILDING2_TO_SALON2010_POSITION = Pair(20, 20)
        val MAIN_TO_SALON2010_POSITION = Pair(25, 25)
        val SALON2010_TO_BUILDING2_POSITION = Pair(5, 5)
        val SALON2010_TO_MAIN_POSITION = Pair(1, 1)
        val MAIN_TO_CAFETERIA_POSITION = Pair(2, 2)
        val CAFETERIA_TO_MAIN_POSITION = Pair(1, 1)

        // Puntos de transición para los nuevos mapas
        // Del mapa principal al primer mapa (Estacionamiento)
        val MAIN_TO_ESTACIONAMIENTO_POSITION = Pair(25, 5)
        val ESTACIONAMIENTO_TO_MAIN_POSITION = Pair(20, 38)


        val MAIN_TO_SALIDAMETRO_POSITION = Pair(2, 2)       // Desde mapa principal
        val SALIDAMETRO_TO_MAIN_POSITION = Pair(1, 1)         // Vuelta al mapa principal
        // Del Estacionamiento al segundo mapa (Tramo Atrás Plaza)
        val ESTACIONAMIENTO_TO_PLAZA_POSITION = Pair(35, 20)
        val PLAZA_TO_ESTACIONAMIENTO_POSITION = Pair(5, 20)
        // edificios ia
        val MAIN_TO_EDIFICIO_IA_BAJO = Pair(31, 21)
        val EDIFICIO_IA_BAJO_TO_MAIN = Pair(5, 20)

        val EDIFICIO_IA_BAJO_TO_MEDIO = Pair(5, 20)
        val MAIN_TO_PALAPAS_IA = Pair(1, 1)
        val MAIN_TO_PALAPAS_ISC_POSITION = Pair(8, 29)
        val PALAPAS_ISC_TO_MAIN_POSITION = Pair(38, 38)
        val MAIN_TO_EDIFICIO_GOBIERNO = Pair(8, 35)
        val EDIFICIO_GOBIERNO_TO_MAIN = Pair(20, 2)
        val MAIN_TO_BIBLIOTECA = Pair(35, 15)
        val BIBLIOTECA_TO_MAIN = Pair(2, 20)

        // NUEVOS PUNTOS DE TRANSICIÓN PARA ESIA
        val ZACATENCO_TO_ESIA_POSITION = Pair(25, 12)
        val ESIA_TO_ZACATENCO_POSITION = Pair(25, 35)


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
                MAP_BUILDING2_PISO1 -> createBuilding2Piso1Matrix()
                MAP_ESTACIONAMIENTO -> createEstacionamientoMatrix()
                MAP_TRAS_PLAZA -> createPlazaMatrix()
                MAP_ZACATENCO -> createZacatencoMatrix()
                MAP_LINDAVISTA -> createLindavistaMatrix()
                MAP_SALIDAMETRO -> createSalidaMetroMatrix() // salida metro
                MAP_CABLEBUS -> createCablebusMatix()
                MAP_EDIFICIO_IA_BAJO-> createEdificioIABajoMatrix()
                MAP_EDIFICIO_IA_MEDIO-> createEdificioIAMedioMatrix()
                MAP_EDIFICIO_IA_ALTO -> createEdificioIAAltoMatrix()
                MAP_PALAPAS_IA -> createPalapasIAMapMatrix()
                MAP_PALAPAS_ISC -> createPalapasISCMatrix()
                MAP_EDIFICIO_GOBIERNO -> createEdificioGobiernoMatrix()
                MAP_BIBLIOTECA -> createBibliotecaMatrix()// Matriz para palapas de ISC
                MAP_ESIA -> createESIAMatrix()
                MAP_ENCB -> createEncbMatrix()
                else -> createDefaultMatrix() // Por defecto, un mapa básico
            }
        }


        /*
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
                    else if (i == 18 && j == 10) {
                        matrix[i][j] = INTERACTIVE // Entrada al edificio 4
                    }
                    else if (i == 4 && j == 11) {
                        matrix[i][j] = INTERACTIVE // Entrada a Zacatenco
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

            matrix[5][25] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            matrix[21][31] = INTERACTIVE // entrar edificio ia
            matrix[29][8] = INTERACTIVE // Entrar a las palapas de ISC
            // Áreas de juego específicas
            // Zona central despejada
            for (i in 15..25) {
                for (j in 15..25) {
                    matrix[i][j] = PATH
                }
            }
            // Añadir punto interactivo para el nuevo mapa de Estacionamiento
            matrix[5][25] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            return matrix
        }

        private fun createCablebusMatix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Configuración de bordes
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                    // Zonas interactivas (edificios, entradas)
                    else if (i == 6 && j == 1) {
                        matrix[i][j] = INTERACTIVE // Entrada a vagon
                    }
                    else if (i == 18 && j == 3) {
                        matrix[i][j] = INTERACTIVE // Entrada a vagon
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

        private fun createZacatencoMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Configuración de bordes
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                    // Zonas interactivas (edificios, entradas)
                    else if (i == 12 && j == 10) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 17 && j == 34) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 12 && j == 25) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 17 && j == 31) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 18 && j == 8) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 16 && j == 5) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 19 && j == 4) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }else if( i == 10 && j == 1){
                        matrix[i][j] == INTERACTIVE
                    }
                    else if(i == 24 && j == 12){
                        matrix[i][j] = INTERACTIVE // Entrada a ENCB
                    }
                    // Obstáculos (árboles, bancas, etc)
                    /**else if (i % 7 == 0 && j % 8 == 0) {
                    matrix[i][j] = INACCESSIBLE
                    }**/
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
        //Mapa de la ENCB
        private fun createEncbMatrix(): Array<Array<Int>> {
            // Inicializar todo como PATH (camino libre)
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // ============================================
            // BORDES EXTERIORES - Fila por fila
            // ============================================

            // Borde superior (fila 0)
            for (j in 0 until MAP_WIDTH) {
                matrix[0][j] = WALL
            }

            // Borde inferior (fila 39)
            for (j in 0 until MAP_WIDTH) {
                matrix[39][j] = WALL
            }

            // Borde izquierdo (columna 0)
            for (i in 0 until MAP_HEIGHT) {
                matrix[i][0] = WALL
            }

            // Borde derecho (columna 39)
            for (i in 0 until MAP_HEIGHT) {
                matrix[i][39] = WALL
            }

            // ============================================
            // SALA 1 - SUPERIOR IZQUIERDA
            // Coordenadas: (3,3) hasta (10,12)
            // ============================================

            // Pared superior sala 1
            matrix[3][3] = WALL; matrix[3][4] = WALL; matrix[3][5] = WALL
            matrix[3][6] = WALL; matrix[3][7] = WALL; matrix[3][8] = WALL
            matrix[3][9] = WALL; matrix[3][10] = WALL; matrix[3][11] = WALL; matrix[3][12] = WALL

            // Pared izquierda sala 1
            matrix[4][3] = WALL; matrix[5][3] = WALL; matrix[6][3] = WALL
            matrix[7][3] = WALL; matrix[8][3] = WALL; matrix[9][3] = WALL; matrix[10][3] = WALL

            // Pared derecha sala 1
            matrix[4][12] = WALL; matrix[5][12] = WALL; matrix[6][12] = WALL
            matrix[7][12] = WALL; matrix[8][12] = WALL; matrix[9][12] = WALL; matrix[10][12] = WALL

            // Pared inferior sala 1 (con puerta ancha de 3 casillas)
            matrix[10][3] = WALL; matrix[10][4] = WALL; matrix[10][5] = WALL
            // Puerta: matrix[10][6], matrix[10][7] y matrix[10][8] quedan como PATH
            matrix[10][9] = WALL; matrix[10][10] = WALL; matrix[10][11] = WALL; matrix[10][12] = WALL

            // Obstáculos internos sala 1 (mesas)
            matrix[5][5] = INACCESSIBLE; matrix[5][6] = INACCESSIBLE
            matrix[5][9] = INACCESSIBLE; matrix[5][10] = INACCESSIBLE
            matrix[8][5] = INACCESSIBLE; matrix[8][6] = INACCESSIBLE
            matrix[8][9] = INACCESSIBLE; matrix[8][10] = INACCESSIBLE

            // ============================================
            // SALA 2 - SUPERIOR CENTRO
            // Coordenadas: (3,15) hasta (10,24)
            // ============================================

            // Pared superior sala 2
            matrix[3][15] = WALL; matrix[3][16] = WALL; matrix[3][17] = WALL
            matrix[3][18] = WALL; matrix[3][19] = WALL; matrix[3][20] = WALL
            matrix[3][21] = WALL; matrix[3][22] = WALL; matrix[3][23] = WALL; matrix[3][24] = WALL

            // Pared izquierda sala 2
            matrix[4][15] = WALL; matrix[5][15] = WALL; matrix[6][15] = WALL
            matrix[7][15] = WALL; matrix[8][15] = WALL; matrix[9][15] = WALL; matrix[10][15] = WALL

            // Pared derecha sala 2
            matrix[4][24] = WALL; matrix[5][24] = WALL; matrix[6][24] = WALL
            matrix[7][24] = WALL; matrix[8][24] = WALL; matrix[9][24] = WALL; matrix[10][24] = WALL

            // Pared inferior sala 2 (con puerta ancha de 3 casillas)
            matrix[10][15] = WALL; matrix[10][16] = WALL; matrix[10][17] = WALL
            // Puerta: matrix[10][18], matrix[10][19] y matrix[10][20] quedan como PATH
            matrix[10][21] = WALL; matrix[10][22] = WALL; matrix[10][23] = WALL; matrix[10][24] = WALL

            // Obstáculos internos sala 2
            matrix[5][17] = INACCESSIBLE; matrix[5][18] = INACCESSIBLE
            matrix[5][21] = INACCESSIBLE; matrix[5][22] = INACCESSIBLE
            matrix[8][17] = INACCESSIBLE; matrix[8][18] = INACCESSIBLE
            matrix[8][21] = INACCESSIBLE; matrix[8][22] = INACCESSIBLE

            // ============================================
            // SALA 3 - SUPERIOR DERECHA
            // Coordenadas: (3,27) hasta (10,36)
            // ============================================

            // Pared superior sala 3
            matrix[3][27] = WALL; matrix[3][28] = WALL; matrix[3][29] = WALL
            matrix[3][30] = WALL; matrix[3][31] = WALL; matrix[3][32] = WALL
            matrix[3][33] = WALL; matrix[3][34] = WALL; matrix[3][35] = WALL; matrix[3][36] = WALL

            // Pared izquierda sala 3
            matrix[4][27] = WALL; matrix[5][27] = WALL; matrix[6][27] = WALL
            matrix[7][27] = WALL; matrix[8][27] = WALL; matrix[9][27] = WALL; matrix[10][27] = WALL

            // Pared derecha sala 3
            matrix[4][36] = WALL; matrix[5][36] = WALL; matrix[6][36] = WALL
            matrix[7][36] = WALL; matrix[8][36] = WALL; matrix[9][36] = WALL; matrix[10][36] = WALL

            // Pared inferior sala 3 (con puerta ancha de 3 casillas)
            matrix[10][27] = WALL; matrix[10][28] = WALL; matrix[10][29] = WALL
            // Puerta: matrix[10][30], matrix[10][31] y matrix[10][32] quedan como PATH
            matrix[10][33] = WALL; matrix[10][34] = WALL; matrix[10][35] = WALL; matrix[10][36] = WALL

            // Obstáculos internos sala 3
            matrix[5][29] = INACCESSIBLE; matrix[5][30] = INACCESSIBLE
            matrix[5][33] = INACCESSIBLE; matrix[5][34] = INACCESSIBLE
            matrix[8][29] = INACCESSIBLE; matrix[8][30] = INACCESSIBLE
            matrix[8][33] = INACCESSIBLE; matrix[8][34] = INACCESSIBLE

            // ============================================
            // ÁREA CENTRAL COMPLETAMENTE ABIERTA
            // Filas 11-28 son PATH (área de juego grande)
            // Ya están como PATH por defecto
            // ============================================

            // ============================================
            // SALA 4 - INFERIOR IZQUIERDA
            // Coordenadas: (29,3) hasta (36,12)
            // ============================================

            // Pared superior sala 4 (con puerta ancha de 3 casillas)
            matrix[29][3] = WALL; matrix[29][4] = WALL; matrix[29][5] = WALL
            // Puerta: matrix[29][6], matrix[29][7] y matrix[29][8] quedan como PATH
            matrix[29][9] = WALL; matrix[29][10] = WALL; matrix[29][11] = WALL; matrix[29][12] = WALL

            // Pared izquierda sala 4
            matrix[30][3] = WALL; matrix[31][3] = WALL; matrix[32][3] = WALL
            matrix[33][3] = WALL; matrix[34][3] = WALL; matrix[35][3] = WALL; matrix[36][3] = WALL

            // Pared derecha sala 4
            matrix[30][12] = WALL; matrix[31][12] = WALL; matrix[32][12] = WALL
            matrix[33][12] = WALL; matrix[34][12] = WALL; matrix[35][12] = WALL; matrix[36][12] = WALL

            // Pared inferior sala 4
            matrix[36][3] = WALL; matrix[36][4] = WALL; matrix[36][5] = WALL
            matrix[36][6] = WALL; matrix[36][7] = WALL; matrix[36][8] = WALL
            matrix[36][9] = WALL; matrix[36][10] = WALL; matrix[36][11] = WALL; matrix[36][12] = WALL

            // Obstáculos internos sala 4 (laboratorio - mesas de trabajo)
            matrix[31][5] = INACCESSIBLE; matrix[31][6] = INACCESSIBLE; matrix[31][7] = INACCESSIBLE
            matrix[31][9] = INACCESSIBLE; matrix[31][10] = INACCESSIBLE

            matrix[34][5] = INACCESSIBLE; matrix[34][6] = INACCESSIBLE; matrix[34][7] = INACCESSIBLE
            matrix[34][9] = INACCESSIBLE; matrix[34][10] = INACCESSIBLE

            // ============================================
            // SALA 5 - INFERIOR CENTRO
            // Coordenadas: (29,15) hasta (36,24)
            // ============================================

            // Pared superior sala 5 (con puerta ancha de 3 casillas)
            matrix[29][15] = WALL; matrix[29][16] = WALL; matrix[29][17] = WALL
            // Puerta: matrix[29][18], matrix[29][19] y matrix[29][20] quedan como PATH
            matrix[29][21] = WALL; matrix[29][22] = WALL; matrix[29][23] = WALL; matrix[29][24] = WALL

            // Pared izquierda sala 5
            matrix[30][15] = WALL; matrix[31][15] = WALL; matrix[32][15] = WALL
            matrix[33][15] = WALL; matrix[34][15] = WALL; matrix[35][15] = WALL; matrix[36][15] = WALL

            // Pared derecha sala 5
            matrix[30][24] = WALL; matrix[31][24] = WALL; matrix[32][24] = WALL
            matrix[33][24] = WALL; matrix[34][24] = WALL; matrix[35][24] = WALL; matrix[36][24] = WALL

            // Pared inferior sala 5
            matrix[36][15] = WALL; matrix[36][16] = WALL; matrix[36][17] = WALL
            matrix[36][18] = WALL; matrix[36][19] = WALL; matrix[36][20] = WALL
            matrix[36][21] = WALL; matrix[36][22] = WALL; matrix[36][23] = WALL; matrix[36][24] = WALL

            // Obstáculos internos sala 5 (cafetería - mesas)
            matrix[31][17] = INACCESSIBLE; matrix[31][18] = INACCESSIBLE
            matrix[31][21] = INACCESSIBLE; matrix[31][22] = INACCESSIBLE

            matrix[34][17] = INACCESSIBLE; matrix[34][18] = INACCESSIBLE
            matrix[34][21] = INACCESSIBLE; matrix[34][22] = INACCESSIBLE

            // ============================================
            // SALA 6 - INFERIOR DERECHA
            // Coordenadas: (29,27) hasta (36,36)
            // ============================================

            // Pared superior sala 6 (con puerta ancha de 3 casillas)
            matrix[29][27] = WALL; matrix[29][28] = WALL; matrix[29][29] = WALL
            // Puerta: matrix[29][30], matrix[29][31] y matrix[29][32] quedan como PATH
            matrix[29][33] = WALL; matrix[29][34] = WALL; matrix[29][35] = WALL; matrix[29][36] = WALL

            // Pared izquierda sala 6
            matrix[30][27] = WALL; matrix[31][27] = WALL; matrix[32][27] = WALL
            matrix[33][27] = WALL; matrix[34][27] = WALL; matrix[35][27] = WALL; matrix[36][27] = WALL

            // Pared derecha sala 6
            matrix[30][36] = WALL; matrix[31][36] = WALL; matrix[32][36] = WALL
            matrix[33][36] = WALL; matrix[34][36] = WALL; matrix[35][36] = WALL; matrix[36][36] = WALL

            // Pared inferior sala 6
            matrix[36][27] = WALL; matrix[36][28] = WALL; matrix[36][29] = WALL
            matrix[36][30] = WALL; matrix[36][31] = WALL; matrix[36][32] = WALL
            matrix[36][33] = WALL; matrix[36][34] = WALL; matrix[36][35] = WALL; matrix[36][36] = WALL

            // Obstáculos internos sala 6 (biblioteca - estanterías)
            matrix[31][29] = INACCESSIBLE; matrix[31][30] = INACCESSIBLE
            matrix[31][33] = INACCESSIBLE; matrix[31][34] = INACCESSIBLE

            matrix[34][29] = INACCESSIBLE; matrix[34][30] = INACCESSIBLE
            matrix[34][33] = INACCESSIBLE; matrix[34][34] = INACCESSIBLE

            // ============================================
            // ALGUNOS OBSTÁCULOS DECORATIVOS EN ÁREA CENTRAL
            // Para hacer el juego más interesante
            // ============================================

            // Jardín/bancas en área central superior
            matrix[15][8] = INACCESSIBLE; matrix[15][9] = INACCESSIBLE
            matrix[15][19] = INACCESSIBLE; matrix[15][20] = INACCESSIBLE
            matrix[15][30] = INACCESSIBLE; matrix[15][31] = INACCESSIBLE

            // Jardín/bancas en área central inferior
            matrix[24][8] = INACCESSIBLE; matrix[24][9] = INACCESSIBLE
            matrix[24][19] = INACCESSIBLE; matrix[24][20] = INACCESSIBLE
            matrix[24][30] = INACCESSIBLE; matrix[24][31] = INACCESSIBLE

            // Algunas columnas/pilares en el centro
            matrix[17][20] = INACCESSIBLE
            matrix[22][20] = INACCESSIBLE

            // ============================================
            // PUNTOS INTERACTIVOS
            // ============================================

            // Marcar puertas como interactivas (centro de cada puerta de 3 casillas)
            matrix[10][7] = INTERACTIVE  // Puerta sala 1
            matrix[10][19] = INTERACTIVE // Puerta sala 2
            matrix[10][31] = INTERACTIVE // Puerta sala 3
            matrix[29][7] = INTERACTIVE  // Puerta sala 4
            matrix[29][19] = INTERACTIVE // Puerta sala 5
            matrix[29][31] = INTERACTIVE // Puerta sala 6

            // Salida principal a Zacatenco (entrada más ancha)
            matrix[20][0] = INTERACTIVE
            matrix[19][0] = INTERACTIVE
            matrix[21][0] = INTERACTIVE

            Log.d("MapMatrix", "Matriz ENCB creada con 6 salas y área central grande - ${MAP_WIDTH}x${MAP_HEIGHT}")
            return matrix
        }

        private fun createLindavistaMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Configuración de bordes
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                    // Zonas interactivas (edificios, entradas)
                    else if (i == 6 && j == 1) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESCOM
                    }
                    else if (i == 34 && j == 33) {
                        matrix[i][j] = INTERACTIVE // Indios Verdes
                    }
                    else if (i == 23 && j == 30) {
                        matrix[i][j] = INTERACTIVE // Plaza
                    }
                    else if (i == 9 && j == 30) {
                        matrix[i][j] = INTERACTIVE // Talleres
                    }
                    // Obstáculos (árboles, bancas, etc)
                    /**else if (i % 7 == 0 && j % 8 == 0) {
                    matrix[i][j] = INACCESSIBLE
                    }**/
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
         * NUEVO MAPA: Estacionamiento de ESCOM
         */
        private fun createEstacionamientoMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } } // Todo es muro por defecto

            // Área del estacionamiento (caminable)
            for (i in 5 until MAP_HEIGHT-5) {
                for (j in 5 until MAP_WIDTH-5) {
                    matrix[i][j] = PATH
                }
            }

            // Líneas de aparcamiento (obstáculos)
            for (row in 0..3) {
                val rowY = 10 + (row * 7)

                // Crear líneas horizontales de autos estacionados
                for (j in 8 until MAP_WIDTH-8) {
                    if (j % 5 == 0) { // Espaciado entre autos
                        matrix[rowY][j] = INACCESSIBLE
                        matrix[rowY+1][j] = INACCESSIBLE
                        matrix[rowY+2][j] = INACCESSIBLE
                    }
                }
            }

            // Caseta de vigilancia (obstáculo)
            for (i in 30..33) {
                for (j in 15..20) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            // Punto interactivo para salir al mapa principal
            matrix[38][20] = INTERACTIVE

            // Punto interactivo para ir al siguiente mapa (TramoAtrasPlaza)
            matrix[20][35] = INTERACTIVE

            return matrix
        }

        /**
         * NUEVO MAPA: Tramo Atrás Plaza
         */
        private fun createPlazaMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } } // Todo es muro por defecto

            // Crear un camino principal que atraviese el mapa
            for (i in 18..22) { // Camino horizontal en el centro
                for (j in 0 until MAP_WIDTH) {
                    matrix[i][j] = PATH
                }
            }

            // Crear áreas verdes (obstáculos)
            for (i in 5..15) {
                for (j in 5..15) {
                    matrix[i][j] = INACCESSIBLE // Área verde superior izquierda
                }
            }

            for (i in 25..35) {
                for (j in 25..35) {
                    matrix[i][j] = INACCESSIBLE // Área verde inferior derecha
                }
            }

            // Bancas en el camino (obstáculos pequeños)
            for (j in 10..30 step 10) {
                matrix[17][j] = INACCESSIBLE
                matrix[23][j] = INACCESSIBLE
            }

            // Punto interactivo para regresar al Estacionamiento
            matrix[20][5] = INTERACTIVE

            // Punto interactivo para ir al siguiente mapa (TramoLindavista)
            matrix[20][35] = INTERACTIVE

            // Añadir un easter egg interactivo
            matrix[10][30] = INTERACTIVE

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

            matrix[15][17] = INTERACTIVE
            matrix[15][18] = INTERACTIVE
            matrix[15][16] = INTERACTIVE

            matrix[1][1] = INTERACTIVE
            // Add labels to help with debugging
            Log.d("MapMatrix", "Interactive value = $INTERACTIVE")
            Log.d("MapMatrix", "Wall value = $WALL")
            Log.d("MapMatrix", "Path value = $PATH")
            Log.d("MapMatrix", "Value at (29, 22): ${matrix[22][29]}")
            Log.d("MapMatrix", "Value at (29, 23): ${matrix[23][29]}")

            return matrix
        }

        private fun createBuilding2Piso1Matrix(): Array<Array<Int>> {
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

                }
            }

            // Make stairs area in room 3

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

            // Key interactive points (salon 2006 entrance)
            // Explicitly set coordinates 29,22 and 29,23 as blue interactive points

            matrix[23][17] = INTERACTIVE
            // Add labels to help with debugging
            Log.d("MapMatrix", "Interactive value = $INTERACTIVE")
            Log.d("MapMatrix", "Wall value = $WALL")
            Log.d("MapMatrix", "Path value = $PATH")
            Log.d("MapMatrix", "Value at (29, 22): ${matrix[22][29]}")
            Log.d("MapMatrix", "Value at (29, 23): ${matrix[23][29]}")

            return matrix
        }
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

        private fun createSalidaMetroMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Constantes
            val PARED = WALL
            val BANCA = INACCESSIBLE


            // Bordes exteriores
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = PARED
                    }
                }
            }

            matrix[5][35] = INTERACTIVE
            matrix[22][17] = INTERACTIVE
            matrix[27][31] = INTERACTIVE
            // Pared al 70% de la altura desde arriba (equivale a 30% desde abajo)
            val alturaPared = (MAP_HEIGHT * 0.7).toInt() // 28 en un mapa 40x40
            for (j in 0 until MAP_WIDTH) {
                matrix[alturaPared][j] = PARED
            }

            // Rectángulo inaccesible (cuadro) con esquinas en (6,1), (6,21), (29,21) y (29,1)
            for (i in 1..21) {
                for (j in 6..29) {
                    matrix[i][j] = BANCA
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




        private fun createEdificioIABajoMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Borde simple
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }
            matrix[18][33] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            return matrix
        }

        private fun createEdificioIAMedioMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Borde simple
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }
            matrix[18][33] = INTERACTIVE // Entrada al Estacionamiento de ESCOM
            matrix[18][36] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            return matrix
        }
        private fun createEdificioIAAltoMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Borde simple
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }
            matrix[18][36] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            return matrix
        }

        /* Matriz para el mapa de palapas de IA del campus */
        private fun createPalapasIAMapMatrix(): Array<Array<Int>> {
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
                    else if (i == 4 && j == 11) {
                        matrix[i][j] = INTERACTIVE // Entrada a Zacatenco
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

            matrix[5][25] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            matrix[21][31] = INTERACTIVE // entrar edificio ia
            // Áreas de juego específicas
            // Zona central despejada
            for (i in 15..25) {
                for (j in 15..25) {
                    matrix[i][j] = PATH
                }
            }
            // Añadir punto interactivo para el nuevo mapa de Estacionamiento
            matrix[5][25] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            return matrix
        }

        // Función para crear la matriz de palapas ISC
        private fun createPalapasISCMatrix(): Array<Array<Int>> {
            // Empezamos con una matriz donde todo es un camino (PATH) por defecto
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Dibuja los muros exteriores
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }
            // Mesa
            for (i in 8..11) {
                for (j in 4..7) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Mesa
            for (i in 8..11) {
                for (j in 14..17) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Mesa
            for (i in 8..11) {
                for (j in 24..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Mesa
            for (i in 21..24) {
                for (j in 9..11) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Mesa
            for (i in 21..24) {
                for (j in 18..21) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Mesa
            for (i in 21..24) {
                for (j in 28..31) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // jardinera
            for (i in 31..32) {
                for (j in 2..15) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // jardinera
            for (i in 31..32) {
                for (j in 20..33) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // jardinera
            for (i in 6..25) {
                for (j in 35..36) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Define el punto de salida (INTERACTIVE) para volver al mapa principal
            // Usamos las coordenadas que definimos antes
            val exitX = PALAPAS_ISC_TO_MAIN_POSITION.first
            val exitY = PALAPAS_ISC_TO_MAIN_POSITION.second
            matrix[exitY][exitX] = INTERACTIVE

            return matrix
        }
        /**
         * NUEVO MAPA: Edificio Gobierno
         */
        /**
         * NUEVO MAPA: Edificio Gobierno
         */
        private fun createEdificioGobiernoMatrix(): Array<Array<Int>> {
            // Empezar con todo como PATH (caminable) en lugar de WALL
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Solo los bordes exteriores son muros
            for (i in 0 until MAP_HEIGHT) {
                matrix[i][0] = WALL
                matrix[i][MAP_WIDTH - 1] = WALL
            }
            for (j in 0 until MAP_WIDTH) {
                matrix[0][j] = WALL
                matrix[MAP_HEIGHT - 1][j] = WALL
            }

            // Oficinas y obstáculos basados en la imagen
            // Oficina superior izquierda
            // Sala superior izquierda - 6 mesas separadas (ejemplo)

// Mesa 1 (arriba-izquierda)
            for (i in 2..4) {
                for (j in 2..4) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

// Mesa 2 (arriba-centro)
            for (i in 2..4) {
                for (j in 6..8) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

// Mesa 3 (arriba-derecha)
            for (i in 2..4) {
                for (j in 10..12) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

// Mesa 4 (abajo-izquierda)
            for (i in 9..11) {
                for (j in 2..4) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

// Mesa 5 (abajo-centro)
            for (i in 9..11) {
                for (j in 6..8) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

// Mesa 6 (abajo-derecha)
            for (i in 9..11) {
                for (j in 10..12) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            //Colision de auditorio,gestion y enfermeria
            for (i in 14..38) {
                for (j in 1..23) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            //recepcion
            for (i in 11..12) {
                for (j in 17..23) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            //mochilas
            for (i in 18..20) {
                for (j in 17..23) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            //mesa
            for (i in 24..27) {
                for (j in 30..38) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 33..36) {
                for (j in 31..38) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            //
            for (i in 1..3) {
                for (j in 21..23) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
//mesas sillon
            for (i in 4..5) {
                for (j in 33..37) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 7..7) {
                for (j in 33..35) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 6..8) {
                for (j in 36..38) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 7..9) {
                for (j in 28..29) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 28..29) {
                for (j in 25..26) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 36..37) {
                for (j in 25..26) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 30..30) {
                for (j in 33..33) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 30..30) {
                for (j in 35..35) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 30..30) {
                for (j in 37..37) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 16..16) {
                for (j in 25..25) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 19..19) {
                for (j in 25..25) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 21..21) {
                for (j in 25..25) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 38..38) {
                for (j in 35..35) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 38..38) {
                for (j in 37..37) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 38..38) {
                for (j in 38..38) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 13..13) {
                for (j in 32..32) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 13..13) {
                for (j in 37..37) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 10..10) {
                for (j in 37..37) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 10..10) {
                for (j in 36..36) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 10..10) {
                for (j in 34..34) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 15..22) {
                for (j in 29..30) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
//paredes
            for (i in 23..23) {
                for (j in 24..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            for (i in 6..13) {
                for (j in 17..17) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 14..22) {
                for (j in 27..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 27..32) {
                for (j in 27..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 32..32) {
                for (j in 24..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 36..38) {
                for (j in 27..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 4..4) {
                for (j in 26..28) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 1..4) {
                for (j in 27..27) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 11..11) {
                for (j in 30..34) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 11..14) {
                for (j in 34..34) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 14..14) {
                for (j in 31..38) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            for (i in 11..11) {
                for (j in 37..38) {
                    matrix[i][j] = INACCESSIBLE
                }
            }






            // Punto interactivo para salir al mapa principal
            matrix[2][20] = INTERACTIVE

            return matrix
        }

        /**
         * NUEVO MAPA: Biblioteca
         */
        private fun createBibliotecaMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } }

            // Área principal de la biblioteca (caminable)
            for (i in 5 until MAP_HEIGHT - 5) {
                for (j in 5 until MAP_WIDTH - 5) {
                    matrix[i][j] = PATH
                }
            }

            // Estanterías de libros (obstáculos en forma de filas)
            for (row in 0..3) {
                val shelfY = 8 + (row * 8)

                // Crear filas de estanterías
                for (j in 8 until MAP_WIDTH - 8) {
                    if (j % 6 < 4) { // Espaciado entre estanterías
                        matrix[shelfY][j] = INACCESSIBLE
                        matrix[shelfY + 1][j] = INACCESSIBLE
                    }
                }
            }

            // Área de estudio
            for (i in 25..30) {
                for (j in 10..30) {
                    if ((i - 25) % 3 == 0 || (j - 10) % 5 == 0) {
                        matrix[i][j] = INACCESSIBLE // Mesas de estudio
                    }
                }
            }

            // Recepción
            for (i in 5..8) {
                for (j in 15..20) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            // Punto interactivo para salir al mapa principal
            matrix[20][2] = INTERACTIVE

            // Puntos interactivos para libros especiales
            matrix[12][12] = INTERACTIVE
            matrix[12][25] = INTERACTIVE
            matrix[20][18] = INTERACTIVE

            return matrix
        }

        private fun createESIAMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } }

            // Crear un área rectangular simple y grande para toda la ESIA
            for (i in 3 until 37) {
                for (j in 3 until 37) {
                    matrix[i][j] = PATH
                }
            }

            // Bloquear la zona superior derecha (figura roja grande)
            for (i in 3 until 12) {
                for (j in 20 until 37) {
                    val diagonal = (i - 3) + (j - 20)
                    if (diagonal > 6) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // Bloquear más zona superior derecha (extensión de la figura roja)
            for (i in 3 until 8) {
                for (j in 15 until 37) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear la zona media derecha (figura verde compleja)
            for (i in 15 until 25) {
                for (j in 28 until 37) {
                    matrix[i][j] = WALL
                }
            }

            // Extensión adicional de la zona verde (parte más irregular)
            for (i in 18 until 22) {
                for (j in 25 until 28) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear el área verde específica que encerraste (zona superior derecha)
            for (i in 8 until 15) {
                for (j in 25 until 37) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (20,12) a (24,12)
            for (i in 12 until 13) {
                for (j in 20 until 25) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (20,13) a (24,13)
            for (i in 13 until 14) {
                for (j in 20 until 25) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (20,14) a (24,14)
            for (i in 14 until 15) {
                for (j in 20 until 25) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (21,15) a (27,15)
            for (i in 15 until 16) {
                for (j in 21 until 28) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (22,16) a (27,16)
            for (i in 16 until 17) {
                for (j in 22 until 28) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (23,17) a (27,17)
            for (i in 17 until 18) {
                for (j in 23 until 28) {
                    matrix[i][j] = WALL
                }
            }

            // NUEVO: Bloquear puntos específicos adicionales
            // Punto (29, 25)
            matrix[25][29] = WALL

            // Punto (27, 22)
            matrix[22][27] = WALL

            // Punto (24, 18)
            matrix[18][24] = WALL

            // NUEVO: Bloquear área (16,8) a (21,8)
            for (i in 8 until 9) {
                for (j in 16 until 22) {
                    matrix[i][j] = WALL
                }
            }

            // NUEVO: Bloquear área (17,9) a (20,9)
            for (i in 9 until 10) {
                for (j in 17 until 21) {
                    matrix[i][j] = WALL
                }
            }

            // NUEVO: Bloquear área (18,10) a (19,10)
            for (i in 10 until 11) {
                for (j in 18 until 20) {
                    matrix[i][j] = WALL
                }
            }

            // NUEVO: Bloquear punto (19,11)
            matrix[11][19] = WALL

            // Bloquear la zona inferior derecha (triángulo inferior)
            for (i in 25 until 37) {
                for (j in 30 until 37) {
                    val diagonal = (37 - i) + (j - 30)
                    if (diagonal > 8) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // Bloquear figura negra superior izquierda
            for (i in 3 until 10) {
                for (j in 3 until 12) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear figura negra superior derecha (zona más específica)
            for (i in 3 until 7) {
                for (j in 12 until 20) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear el rectángulo superior final
            for (i in 3 until 8) {
                for (j in 8 until 30) {
                    matrix[i][j] = WALL
                }
            }

            // Punto de salida hacia Zacatenco (puerta principal en la parte inferior)
            matrix[35][25] = INTERACTIVE

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
                if(x == 1 && y == 1){
                    return MAP_MAIN
                }
            }

            if(mapId == MAP_BUILDING2_PISO1){
                if(x == 17 && y == 23){
                    return MAP_BUILDING2
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

            if(mapId == MAP_BUILDING2 && x == 17 && y == 15){
                return MAP_BUILDING2_PISO1
            }

            if (mapId == MAP_MAIN && x == 33 && y == 34) {
                return MAP_CAFETERIA
            }

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
            // Transiciones desde el Estacionamiento
            if (mapId == MAP_ESTACIONAMIENTO) {
                // Regresar al mapa principal
                if (x == 20 && y == 38) return MAP_MAIN

                // Ir al siguiente mapa (Tramo Atrás Plaza)
                if (x == 35 && y == 20) return MAP_TRAS_PLAZA
            }


            if (mapId == Companion.MAP_CABLEBUS) {
                if (x == 5 && y == 5) {
                    return MAP_LINDAVISTA
                }
//                if (x == 10 && y == 10) {
//                    return MAP_MAIN
//                }
            }

            if (mapId == MAP_MAIN && x == 31 && y == 21) {
                return MAP_CAFETERIA
            }

            if (mapId == MAP_MAIN && x == 31 && y == 10) {
                return MAP_PALAPAS_IA
            }
            // Transición DESDE el mapa principal HACIA las palapas ISC
            if (mapId == MAP_MAIN && x == MAIN_TO_PALAPAS_ISC_POSITION.first && y == MAIN_TO_PALAPAS_ISC_POSITION.second) {
                return MAP_PALAPAS_ISC
            }

            // Transición DESDE las palapas ISC de vuelta HACIA el mapa principal
            if (mapId == MAP_PALAPAS_ISC && x == PALAPAS_ISC_TO_MAIN_POSITION.first && y == PALAPAS_ISC_TO_MAIN_POSITION.second) {
                return MAP_MAIN
            }

            // Nuevas transiciones para Edificio Gobierno y Biblioteca
            if (mapId == MAP_MAIN && x == 8 && y == 35) {
                return MAP_EDIFICIO_GOBIERNO
            }

            if (mapId == MAP_EDIFICIO_GOBIERNO && x == 20 && y == 2) {
                return MAP_MAIN
            }

            if (mapId == MAP_MAIN && x == 15 && y == 35) {
                return MAP_BIBLIOTECA
            }

            if (mapId == MAP_BIBLIOTECA && x == 2 && y == 20) {
                return MAP_MAIN
            }

            // TRANSICIONES PARA ESIA
            if (mapId == MAP_ESIA && x == 25 && y == 35) {
                return MAP_ZACATENCO
            }

            if (mapId == MAP_ZACATENCO && x == 25 && y == 12) {
                return MAP_ESIA
            }

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
                MAP_BUILDING2_PISO1 -> Pair(20, 16)  // Posición central dentro del salón 2009
                MAP_CABLEBUS -> Pair(2, 2) // Posicion central dentro del cablebus
                MAP_EDIFICIO_IA_BAJO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_EDIFICIO_IA_MEDIO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_EDIFICIO_IA_ALTO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_PALAPAS_IA -> Pair(2, 2)

                MAP_PALAPAS_ISC -> Pair(38, 38) // Posición inicial dentro de palapas ISC
                MAP_EDIFICIO_GOBIERNO -> Pair(17, 5)  // Posición cerca de la entrada
                MAP_BIBLIOTECA -> Pair(17, 5)  // Posición cerca de la entrada
                MAP_ESIA -> Pair(25, 35) // Posición inicial en ESIA (cerca de la entrada)
                else -> Pair(MAP_WIDTH / 2, MAP_HEIGHT / 2)
            }
        }
    }
}