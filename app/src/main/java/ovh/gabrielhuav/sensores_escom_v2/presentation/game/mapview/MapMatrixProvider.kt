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
        const val MAP_BUILDING2_PISO2 = "escom_building2_piso2"
        const val MAP_BUILDING4_F2 = "escom_building4_floor_2"
        const val MAP_SALON2001 = "escom_salon2001"
        const val MAP_SALON2002 = "escom_salon2002"
        const val MAP_SALON2003 = "escom_salon2003"
        const val MAP_SALON2004 = "escom_salon2004"
        const val MAP_SALON2005 = "escom_salon2005"
        const val MAP_SALON2006 = "escom_salon2006"
        const val MAP_SALON2101 = "escom_salon2101"
        const val MAP_SALON2102 = "escom_salon2102"
        const val MAP_SALON2103 = "escom_salon2103"
        const val MAP_SALON2104 = "escom_salon2104"
        const val MAP_SALON2105 = "escom_salon2105"
        const val MAP_SALON2106 = "escom_salon2106"
        const val MAP_SALON2201 = "escom_salon2201"
        const val MAP_SALON2202 = "escom_salon2202"
        const val MAP_SALON2203 = "escom_salon2203"
        const val MAP_SALON2204 = "escom_salon2204"
        const val MAP_SALON2205 = "escom_salon2205"
        const val MAP_SALON2206 = "escom_salon2206"
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
        const val MAP_SALON4102 = "escom_salon4102"
        const val MAP_CABLEBUS = "cablebus"
        const val MAP_SALIDAMETRO = "escom_salidametro"
        const val MAP_METRO_POLITECNICO = "metro_politecnico"
        const val MAP_ANDENES_METRO_POLITECNICO = "andenes_metro_politecnico"
        const val MAP_RED_METRO = "red_metro"
        const val MAP_PALAPAS_IA = "escom_palapas_ia"

        const val MAP_CANCHA_IA = "escom_cancha_ia"
        const val MAP_ESIME = "esime_zacatenco"
        const val MAP_PALAPAS_ISC = "escom_palapas_isc"
        const val MAP_EDIFICIO_GOBIERNO = "escom_edificio_gobierno"
        const val MAP_BIBLIOTECA = "escom_biblioteca"
        const val MAP_ENCB = "encb"
        const val MAP_PLAZA_TORRES = "plaza_torres"
        const val MAP_PLAZA_TORRES_N1 = "plaza_torres_n1"
        const val MAP_LAB_POSGRADO = "escom_lab_posgrado"
        const val MAP_ESIA = "escom_esia"
        const val MAP_CIDETEC = "escom_cidetec"
        const val MAP_PLAZA_VISTA_NORTE = "plazaVistaNorte"
        const val MAP_BIBLIOTECA_ESIA = "biblioteca_esia"
        const val MAP_EDIFICIO_ESIA = "edificio_esia"
        const val MAP_SALON_ESIA="salon_esia"

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
                lowerMap.contains("escom_building2_piso2") -> MAP_BUILDING2_PISO2
                lowerMap.contains("building2") || lowerMap.contains("edificio2") -> MAP_BUILDING2

                // Salones Edificio 2
                //Planta Baja
                lowerMap.contains("2001") || lowerMap.contains("salon2001") -> MAP_SALON2001
                lowerMap.contains("2002") || lowerMap.contains("salon2002") -> MAP_SALON2002
                lowerMap.contains("2003") || lowerMap.contains("salon2003") -> MAP_SALON2003
                lowerMap.contains("2004") || lowerMap.contains("salon2004") -> MAP_SALON2004
                lowerMap.contains("2005") || lowerMap.contains("salon2005") -> MAP_SALON2005
                lowerMap.contains("2006") || lowerMap.contains("salon2006") -> MAP_SALON2006
                //Primer Piso
                lowerMap.contains("2101") || lowerMap.contains("salon2101") -> MAP_SALON2101
                lowerMap.contains("2102") || lowerMap.contains("salon2102") -> MAP_SALON2102
                lowerMap.contains("2103") || lowerMap.contains("salon2103") -> MAP_SALON2103
                lowerMap.contains("2104") || lowerMap.contains("salon2104") -> MAP_SALON2104
                lowerMap.contains("2105") || lowerMap.contains("salon2105") -> MAP_SALON2105
                lowerMap.contains("2106") || lowerMap.contains("salon2106") -> MAP_SALON2106
                //Segundo Piso
                lowerMap.contains("2201") || lowerMap.contains("salon2201") -> MAP_SALON2201
                lowerMap.contains("2202") || lowerMap.contains("salon2202") -> MAP_SALON2202
                lowerMap.contains("2203") || lowerMap.contains("salon2203") -> MAP_SALON2203
                lowerMap.contains("2204") || lowerMap.contains("salon2204") -> MAP_SALON2204
                lowerMap.contains("2205") || lowerMap.contains("salon2205") -> MAP_SALON2205
                lowerMap.contains("2206") || lowerMap.contains("salon2206") -> MAP_SALON2206
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
                lowerMap.contains("plazaVistaNorte") -> MAP_PLAZA_VISTA_NORTE
                // Cidetec
                lowerMap.contains("linda") || lowerMap.contains("lindavista") -> MAP_CIDETEC
                // edificio ia
                lowerMap.contains("ia_baja") || lowerMap.contains("edificio_ia_bajo") -> MAP_EDIFICIO_IA_BAJO
                lowerMap.contains("ia_medio") || lowerMap.contains("edificio_ia_medio") -> MAP_EDIFICIO_IA_MEDIO
                lowerMap.contains("ia_alto") || lowerMap.contains("edificio_ia_alto") -> MAP_EDIFICIO_IA_ALTO
                lowerMap.contains("4102") || lowerMap.contains("salon4102") -> MAP_SALON4102
                lowerMap.contains("cable") || lowerMap.contains("cablebus") -> MAP_CABLEBUS
                lowerMap.contains("palapas_ia") -> MAP_PALAPAS_IA
                lowerMap.contains("cancha") || lowerMap.contains("cancha_ia") -> MAP_CANCHA_IA

                lowerMap.contains("gobierno") || lowerMap.contains("edificio_gobierno") -> MAP_EDIFICIO_GOBIERNO
                lowerMap.contains("biblioteca") -> MAP_BIBLIOTECA

                //ESIME
                lowerMap.contains("esime") || lowerMap.contains("esime_zacatenco") -> MAP_ESIME


                // ESIA
                lowerMap.contains("esia") -> MAP_ESIA
                lowerMap.contains("encb") -> MAP_ENCB
                lowerMap.contains("plaza_torres") -> MAP_PLAZA_TORRES
                lowerMap.contains("plaza_torres_n1") -> MAP_PLAZA_TORRES_N1
                lowerMap.contains("biblioteca_esia") || lowerMap.contains("biblioteca") && lowerMap.contains("esia") -> MAP_BIBLIOTECA_ESIA
                lowerMap.contains("edificio_esia") || (lowerMap.contains("edificio") && lowerMap.contains("esia")) -> MAP_EDIFICIO_ESIA

                // Si no coincide con ninguno de los anteriores, devolver el original
                else -> mapName
            }
        }
        // Puntos de transición entre mapas existentes
        val MAIN_TO_BUILDING2_POSITION = Pair(15, 10)
        val BUILDING2_TO_MAIN_POSITION = Pair(5, 5)

        //Edificio 2 to Salon
        //Planta Baja
        val BUILDING2_TO_SALON2001_POSITION = Pair(4, 22)
        val BUILDING2_TO_SALON2002_POSITION = Pair(9, 22)
        val BUILDING2_TO_SALON2003_POSITION = Pair(13, 22)
        val BUILDING2_TO_SALON2004_POSITION = Pair(21, 22)
        val BUILDING2_TO_SALON2005_POSITION = Pair(26, 22)
        val BUILDING2_TO_SALON2006_POSITION = Pair(31, 22)
        //Primer Piso
        val BUILDING2P1_TO_SALON2101_POSITION = Pair(4, 22)
        val BUILDING2P1_TO_SALON2102_POSITION = Pair(9, 22)
        val BUILDING2P1_TO_SALON2103_POSITION = Pair(13, 22)
        val BUILDING2P1_TO_SALON2104_POSITION = Pair(21, 22)
        val BUILDING2P1_TO_SALON2105_POSITION = Pair(26, 22)
        val BUILDING2P1_TO_SALON2106_POSITION = Pair(31, 22)
        //Segundo Piso
        val BUILDING2P2_TO_SALON2201_POSITION = Pair(4, 22)
        val BUILDING2P2_TO_SALON2202_POSITION = Pair(9, 22)
        val BUILDING2P2_TO_SALON2203_POSITION = Pair(13, 22)
        val BUILDING2P2_TO_SALON2204_POSITION = Pair(21, 22)
        val BUILDING2P2_TO_SALON2205_POSITION = Pair(26, 22)
        val BUILDING2P2_TO_SALON2206_POSITION = Pair(31, 22)

        //Salon to Edificio 2
        //Planta Baja
        val SALON2001_TO_BUILDING2_POSITION = Pair(1, 20)
        val SALON2002_TO_BUILDING2_POSITION = Pair(1, 20)
        val SALON2003_TO_BUILDING2_POSITION = Pair(1, 20)
        val SALON2004_TO_BUILDING2_POSITION = Pair(1, 20)
        val SALON2005_TO_BUILDING2_POSITION = Pair(1, 20)
        val SALON2006_TO_BUILDING2_POSITION = Pair(1, 20)
        //Primer Piso
        val SALON2101_TO_BUILDING2P1_POSITION = Pair(1, 20)
        val SALON2102_TO_BUILDING2P1_POSITION = Pair(1, 20)
        val SALON2103_TO_BUILDING2P1_POSITION = Pair(1, 20)
        val SALON2104_TO_BUILDING2P1_POSITION = Pair(1, 20)
        val SALON2105_TO_BUILDING2P1_POSITION = Pair(1, 20)
        val SALON2106_TO_BUILDING2P1_POSITION = Pair(1, 20)
        //Segundo Piso
        val SALON2201_TO_BUILDING2P2_POSITION = Pair(1, 20)
        val SALON2202_TO_BUILDING2P2_POSITION = Pair(1, 20)
        val SALON2203_TO_BUILDING2P2_POSITION = Pair(1, 20)
        val SALON2204_TO_BUILDING2P2_POSITION = Pair(1, 20)
        val SALON2205_TO_BUILDING2P2_POSITION = Pair(1, 20)
        val SALON2206_TO_BUILDING2P2_POSITION = Pair(1, 20)

        // Puntos de transición para los nuevos mapas
        // Del mapa principal al primer mapa (Estacionamiento)
        val MAIN_TO_CAFETERIA_POSITION = Pair(2, 2)
        val CAFETERIA_TO_MAIN_POSITION = Pair(1, 1)
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

        // Transiciones ESIME - Zacatenco
        val ESIME_TO_ZACATENCO_POSITION = Pair(5, 35)
        val ZACATENCO_TO_ESIME_POSITION = Pair(28, 24)

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
                MAP_BUILDING2_PISO1 -> createBuilding2Piso1Matrix()
                MAP_BUILDING2_PISO2 -> createBuilding2Piso2Matrix()
                MAP_BUILDING4_F2 -> createBuilding2Matrix()
                // Salones Edificio 2
                //Planta Baja
                MAP_SALON2001 -> createSalonMatrix()
                MAP_SALON2002 -> createSalonMatrix()
                MAP_SALON2003 -> createSalonMatrix()
                MAP_SALON2004 -> createSalonMatrix()
                MAP_SALON2005 -> createSalonMatrix()
                MAP_SALON2006 -> createSalonMatrix()
                //Primer Piso
                MAP_SALON2101 -> createSalonMatrix()
                MAP_SALON2102 -> createSalonMatrix()
                MAP_SALON2103 -> createSalonMatrix()
                MAP_SALON2104 -> createSalonMatrix()
                MAP_SALON2105 -> createSalonMatrix()
                MAP_SALON2106 -> createSalonMatrix()
                //Segundo Piso
                MAP_SALON2201 -> createSalonMatrix()
                MAP_SALON2202 -> createSalonMatrix()
                MAP_SALON2203 -> createSalonMatrix()
                MAP_SALON2204 -> createSalonMatrix()
                MAP_SALON2205 -> createSalonMatrix()
                MAP_SALON2206 -> createSalonMatrix()
                MAP_SALON2009 -> createSalon2009Matrix()  // Nueva matriz para el salón 2009
                MAP_SALON2010 -> createSalon2010Matrix()  // Nueva matriz para el salón 2010


                MAP_SALON1212 -> createSalon1212Matrix()
                MAP_CAFETERIA -> createCafeESCOMMatrix()
                MAP_ESTACIONAMIENTO -> createEstacionamientoMatrix()
                MAP_TRAS_PLAZA -> createPlazaMatrix()
                MAP_ZACATENCO -> createZacatencoMatrix()
                MAP_LINDAVISTA -> createLindavistaMatrix()
                MAP_SALIDAMETRO -> createSalidaMetroMatrix() // salida metro
                MAP_METRO_POLITECNICO -> createMetroPolitecnicoMatrix()
                MAP_ANDENES_METRO_POLITECNICO -> createAndenesMetroPolitecnicoMatrix()
                MAP_RED_METRO -> createRedMetroMatrix()
                MAP_CABLEBUS -> createCablebusMatix()
                MAP_EDIFICIO_IA_BAJO-> createEdificioIABajoMatrix()
                MAP_EDIFICIO_IA_MEDIO-> createEdificioIAMedioMatrix()
                MAP_EDIFICIO_IA_ALTO -> createEdificioIAAltoMatrix()
                MAP_PALAPAS_IA -> createPalapasIAMapMatrix()
                MAP_CANCHA_IA -> createCanchaIAMatrix()
                MAP_PALAPAS_ISC -> createPalapasISCMatrix()
                MAP_EDIFICIO_GOBIERNO -> createEdificioGobiernoMatrix()
                MAP_BIBLIOTECA -> createBibliotecaMatrix()
                MAP_ESIA -> createESIAMatrix()
                MAP_ENCB -> createEncbMatrix()
                MAP_ESIME -> createEsimeMatrix()
                MAP_PLAZA_TORRES -> createPlazaTorresMatrix()
                MAP_PLAZA_TORRES_N1 -> createPlazaTorresN1Matrix()
                MAP_ESIME -> createEsimeMatrix()
                MAP_PLAZA_VISTA_NORTE -> createPlazaVistaNorteMatrix()
                MAP_LAB_POSGRADO -> createLabPosgradoMatrix()
                MAP_CIDETEC -> createCidetecMatrix()
                MAP_BIBLIOTECA_ESIA -> createBibliotecaESIAMatrix()
                MAP_EDIFICIO_ESIA -> createEdificioESIAMatrix()
                MAP_SALON_ESIA -> getSalonESIAMatrix()

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
            matrix[12][24] = INTERACTIVE

            matrix[5][25] = INTERACTIVE // Entrada al Estacionamiento de ESCOM

            matrix[21][31] = INTERACTIVE // entrar edificio ia
            matrix[29][8] = INTERACTIVE // Entrar a las palapas de ISC
            // Punto de transición para el mapa global
            matrix[18][14] = INTERACTIVE
            // Áreas de juego específicas
            // Zona central despejada
            for (i in 18..25) {
                for (j in 15..25) {
                    matrix[i][j] = PATH
                }
            }
            // Añadir punto interactivo para el nuevo mapa de Estacionamiento
            matrix[5][25] = INTERACTIVE // Entrada al Estacionamiento de ESCOM
            matrix[28][33] = INTERACTIVE
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
                    else if (i == 24 && j == 28) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESIME
                    }
                    else if (i == 24 && j == 28) {
                        matrix[i][j] = INTERACTIVE // Entrada a ESIME
                    }
                    else if(i == 24 && j == 12){
                        matrix[i][j] = INTERACTIVE // Entrada a ENCB
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

            matrix[10][20] = INTERACTIVE

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

        /**
         * Matriz para el mapa de Plaza Vista Norte
         */
        private fun createPlazaVistaNorteMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Bordes exteriores
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // Jardineras (INACCESSIBLE)
            // Jardinera superior
            for (i in 5..10) {
                for (j in 5..35) {
                    matrix[i][j] = INACCESSIBLE
                }
            }
            // Jardinera inferior
            for (i in 30..35) {
                for (j in 5..35) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            // Bancas (INACCESSIBLE)
            matrix[15][10] = INACCESSIBLE; matrix[15][11] = INACCESSIBLE
            matrix[15][28] = INACCESSIBLE; matrix[15][29] = INACCESSIBLE
            matrix[25][10] = INACCESSIBLE; matrix[25][11] = INACCESSIBLE
            matrix[25][28] = INACCESSIBLE; matrix[25][29] = INACCESSIBLE

            // Puntos interactivos
            // Punto de transición para volver a Lindavista
            matrix[6][1] = INTERACTIVE

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

        // ============================================================
// Función para crear la matriz del mapa de CIDETEC
// ============================================================
        private fun createCidetecMatrix(): Array<Array<Int>> {
            // Empezamos con una matriz donde todo es un camino (PATH) por defecto
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // 1️⃣ Bordes exteriores (muros generales)
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // ============================================================
            // 2️⃣ PAREDES INTERNAS (INACCESSIBLE = 1)
            // Las coordenadas se interpretan como matrix[y][x]
            // ============================================================

            // 🔹 Pared 1: (10,21) → (10,37)
            for (i in 21..37) {
                val j = 10
                matrix[i][j] = INACCESSIBLE
            }

            // 🔹 Pared 2: (10,3) → (10,18)
            for (i in 3..18) {
                val j = 10
                matrix[i][j] = INACCESSIBLE
            }

            // 🔹 Pared 3: (10,37) → (30,37)
            for (j in 10..30) {
                val i = 37
                matrix[i][j] = INACCESSIBLE
            }

            // 🔹 Pared 4: (30,37) → (30,3)
            for (i in 3..37) {
                val j = 30
                matrix[i][j] = INACCESSIBLE
            }

            // 🔹 Pared 5: (10,3) → (30,3)
            for (j in 10..30) {
                val i = 3
                matrix[i][j] = INACCESSIBLE
            }

            // ============================================================
            // 3️⃣ Punto interactivo: salida hacia Zacatenco o entrada
            // ============================================================
            matrix[22][11] = INTERACTIVE // (x=11, y=22)

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


        fun createSalonMatrix(): Array<Array<Int>> {
            // 1. Iniciar la matriz con todo el espacio como transitable (PATH).
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // 2. Dibujar las paredes exteriores del salón.
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // 3. Colocar el pizarrón y la pantalla en la parte superior.
            for (j in 12 until MAP_WIDTH - 12) {
                matrix[6][j] = WALL
            }

            // 4. Colocar el escritorio del profesor.
            for (i in 10..12) {
                for (j in 25..29) {
                    matrix[i][j] = WALL
                }
            }

            // 5. Colocar los pupitres de los estudiantes en una cuadrícula de 5 filas y 7 columnas.
            val numRows = 5
            val numCols = 8
            val rowSpacing = 5 // Espacio vertical entre pupitres
            val colSpacing = 4 // Espacio horizontal entre pupitres
            val startY = 15    // Posición Y inicial
            val startX = 4     // Posición X inicial

            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val deskY = startY + row * rowSpacing
                    val deskX = startX + col * colSpacing
                    // Cada pupitre es un obstáculo (WALL)
                    matrix[deskY][deskX] = WALL
                }
            }

            // 6. Agregar el punto de interacción para la puerta en la esquina superior izquierda.
            matrix[6][0] = INTERACTIVE

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
            // Empezar con una matriz donde todo es un camino (PATH)
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // --- Coordenadas Clave ---
            // Estas son las coordenadas de las paredes que me pediste no mover.
            val topWallY = 14            // Posición Y del muro superior de los salones
            val classroomDepth = 8       // Profundidad de los salones
            val corridorWallY = topWallY + classroomDepth // Muro del pasillo (calculado automáticamente)
            val corridorHeight = 9       // Altura del pasillo
            val bottomWallY = corridorWallY + corridorHeight // Muro inferior del edificio
            val leftWallX = 1            // Borde izquierdo
            val rightWallX = MAP_WIDTH - 4 // Borde derecho

            // --- 1. MUROS EXTERIORES Y DEL PASILLO ---
            // Dibuja los 4 muros principales que forman la caja del edificio y el pasillo.
            for (j in leftWallX..rightWallX) {
                matrix[topWallY][j] = WALL       // Muro superior
                matrix[corridorWallY][j] = WALL  // Muro del pasillo (con las puertas)
                matrix[bottomWallY][j] = WALL    // Muro inferior
            }
            for (i in topWallY..bottomWallY) {
                matrix[i][leftWallX] = WALL      // Muro izquierdo
                matrix[i][rightWallX] = WALL     // Muro derecho
            }

            // --- 2. PAREDES VERTICALES ENTRE SALONES ---
            // Dibuja las divisiones de cada salón.
            val verticalWallPositions = listOf(6, 11, 15, 19, 23, 28, 33) // Coordenadas X de cada pared
            for (wallX in verticalWallPositions) {
                for (i in topWallY..corridorWallY) {
                    matrix[i][wallX] = WALL
                }
            }

            // --- 3. PUERTAS Y PUNTOS INTERACTIVOS (CORREGIDO) ---
            // Abrimos huecos en la pared del pasillo y los marcamos como interactivos.
            matrix[corridorWallY][2] = INTERACTIVE     // Puerta Salón 2001
            matrix[corridorWallY][7] = INTERACTIVE     // Puerta Salón 2002
            matrix[corridorWallY][12] = INTERACTIVE     // Puerta Salón 2002
            matrix[corridorWallY][16] = INTERACTIVE    // Puerta Salón 2003

            // Escaleras: Abrimos un hueco y ponemos un punto interactivo en el centro.
            for(j in 16..18) { matrix[corridorWallY][j] = PATH }
            matrix[corridorWallY - 1][17] = INTERACTIVE // Punto para subir

            matrix[corridorWallY][20] = INTERACTIVE    // Puerta Salón 2004
            matrix[corridorWallY][24] = INTERACTIVE    // Puerta Salón 2005
            matrix[corridorWallY][29] = INTERACTIVE    // Puerta Salón 2006
            matrix[corridorWallY][34] = INTERACTIVE    // Puerta Baños (WC)

            // --- 4. PUNTO DE SALIDA ---
            // Punto para volver al mapa principal.
            matrix[corridorWallY + 2][leftWallX] = INTERACTIVE

            Log.d("MapMatrix", "Matriz del Edificio 2 (Final) creada y alineada.")
            return matrix
        }

        private fun createBuilding2Piso1Matrix(): Array<Array<Int>> {
                // Empezar con una matriz donde todo es un camino (PATH)
                val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

                // --- Coordenadas Clave ---
                // Estas son las coordenadas de las paredes que me pediste no mover.
                val topWallY = 14            // Posición Y del muro superior de los salones
                val classroomDepth = 8       // Profundidad de los salones
                val corridorWallY = topWallY + classroomDepth // Muro del pasillo (calculado automáticamente)
                val corridorHeight = 9       // Altura del pasillo
                val bottomWallY = corridorWallY + corridorHeight // Muro inferior del edificio
                val leftWallX = 1            // Borde izquierdo
                val rightWallX = MAP_WIDTH - 4 // Borde derecho

                // --- 1. MUROS EXTERIORES Y DEL PASILLO ---
                // Dibuja los 4 muros principales que forman la caja del edificio y el pasillo.
                for (j in leftWallX..rightWallX) {
                    matrix[topWallY][j] = WALL       // Muro superior
                    matrix[corridorWallY][j] = WALL  // Muro del pasillo (con las puertas)
                    matrix[bottomWallY][j] = WALL    // Muro inferior
                }
                for (i in topWallY..bottomWallY) {
                    matrix[i][leftWallX] = WALL      // Muro izquierdo
                    matrix[i][rightWallX] = WALL     // Muro derecho
                }

                // --- 2. PAREDES VERTICALES ENTRE SALONES ---
                // Dibuja las divisiones de cada salón.
                val verticalWallPositions = listOf(6, 11, 15, 19, 23, 28, 33) // Coordenadas X de cada pared
                for (wallX in verticalWallPositions) {
                    for (i in topWallY..corridorWallY) {
                        matrix[i][wallX] = WALL
                    }
                }

                // --- 3. PUERTAS Y PUNTOS INTERACTIVOS (CORREGIDO) ---
                // Abrimos huecos en la pared del pasillo y los marcamos como interactivos.
                matrix[corridorWallY][2] = INTERACTIVE     // Puerta Salón 2001
                matrix[corridorWallY][7] = INTERACTIVE     // Puerta Salón 2002
                matrix[corridorWallY][12] = INTERACTIVE     // Puerta Salón 2002
                matrix[corridorWallY][16] = INTERACTIVE    // Puerta Salón 2003

                // Escaleras: Abrimos un hueco y ponemos un punto interactivo en el centro.
                for(j in 16..18) { matrix[corridorWallY][j] = PATH }
                matrix[corridorWallY - 1][17] = INTERACTIVE // Punto para bajar
                matrix[corridorWallY - 3][17] = INTERACTIVE // Punto para subir


            matrix[corridorWallY][20] = INTERACTIVE    // Puerta Salón 2004
                matrix[corridorWallY][24] = INTERACTIVE    // Puerta Salón 2005
                matrix[corridorWallY][29] = INTERACTIVE    // Puerta Salón 2006
                matrix[corridorWallY][34] = INTERACTIVE    // Puerta Baños (WC)

                Log.d("MapMatrix", "Matriz del Edificio 2 (Final) creada y alineada.")
                return matrix
        }

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

        private fun createBuilding2Piso2Matrix(): Array<Array<Int>> {
            // Empezar con una matriz donde todo es un camino (PATH)
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // --- Coordenadas Clave ---
            // Estas son las coordenadas de las paredes que me pediste no mover.
            val topWallY = 14            // Posición Y del muro superior de los salones
            val classroomDepth = 8       // Profundidad de los salones
            val corridorWallY = topWallY + classroomDepth // Muro del pasillo (calculado automáticamente)
            val corridorHeight = 9       // Altura del pasillo
            val bottomWallY = corridorWallY + corridorHeight // Muro inferior del edificio
            val leftWallX = 1            // Borde izquierdo
            val rightWallX = MAP_WIDTH - 4 // Borde derecho

            // --- 1. MUROS EXTERIORES Y DEL PASILLO ---
            // Dibuja los 4 muros principales que forman la caja del edificio y el pasillo.
            for (j in leftWallX..rightWallX) {
                matrix[topWallY][j] = WALL       // Muro superior
                matrix[corridorWallY][j] = WALL  // Muro del pasillo (con las puertas)
                matrix[bottomWallY][j] = WALL    // Muro inferior
            }
            for (i in topWallY..bottomWallY) {
                matrix[i][leftWallX] = WALL      // Muro izquierdo
                matrix[i][rightWallX] = WALL     // Muro derecho
            }

            // --- 2. PAREDES VERTICALES ENTRE SALONES ---
            // Dibuja las divisiones de cada salón.
            val verticalWallPositions = listOf(6, 11, 15, 19, 23, 28, 33) // Coordenadas X de cada pared
            for (wallX in verticalWallPositions) {
                for (i in topWallY..corridorWallY) {
                    matrix[i][wallX] = WALL
                }
            }


            // --- 3. PUERTAS Y PUNTOS INTERACTIVOS (CORREGIDO) ---
            // Abrimos huecos en la pared del pasillo y los marcamos como interactivos.
            matrix[corridorWallY][2] = INTERACTIVE     // Puerta Salón 2001
            matrix[corridorWallY][7] = INTERACTIVE     // Puerta Salón 2002
            matrix[corridorWallY][12] = INTERACTIVE     // Puerta Salón 2002
            matrix[corridorWallY][16] = INTERACTIVE    // Puerta Salón 2003

            // Escaleras: Abrimos un hueco y ponemos un punto interactivo en el centro.
            for(j in 16..18) { matrix[corridorWallY][j] = PATH }
            matrix[corridorWallY - 1][17] = INTERACTIVE // Punto para bajar

            matrix[corridorWallY][20] = INTERACTIVE    // Puerta Salón 2004
            matrix[corridorWallY][24] = INTERACTIVE    // Puerta Salón 2005
            matrix[corridorWallY][29] = INTERACTIVE    // Puerta Salón 2006
            matrix[corridorWallY][34] = INTERACTIVE    // Puerta Baños (WC)

            Log.d("MapMatrix", "Matriz del Edificio 2 (Final) creada y alineada.")
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
            matrix[5][37] = INTERACTIVE
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
            //Contorno de la entrada del metro
            val anchoParedMetro = 36
            for (i in 6 until 14) {
                matrix[i][anchoParedMetro] = PARED
            }

            val alturaParedMetro = 14
            for (j in 36 until MAP_WIDTH) {
                matrix[alturaParedMetro][j] = PARED
            }

            return matrix
        }
        private fun createMetroPolitecnicoMatrix(): Array<Array<Int>> {
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
            // Pared arriba y abajo
            for (i in 1..2){
                for(j in 0 until MAP_WIDTH)
                    matrix[i][j] = PARED
            }
            for (i in 37..38){
                for(j in 0 until MAP_WIDTH)
                    matrix[i][j] = PARED
            }

            for (i in 0 until MAP_HEIGHT){
                for(j in 34 until MAP_WIDTH)
                    matrix[i][j] = PARED
            }

            for (i in 0 until 15){
                for(j in 0 until 13)
                    matrix[i][j] = PARED
            }

            for (i in 0 until 15){
                for(j in 24 until MAP_WIDTH)
                    matrix[i][j] = PARED
            }

            for (i in 25 until MAP_HEIGHT){
                for(j in 0 until 12)
                    matrix[i][j] = PARED
            }

            for (i in 31 until MAP_HEIGHT){
                for(j in 19 until MAP_WIDTH)
                    matrix[i][j] = PARED
            }
            var altura = 30
            for(j in 20 until MAP_WIDTH)
                matrix[altura][j] = PARED
            altura = 29
            for(j in 21 until MAP_WIDTH)
                matrix[altura][j] = PARED

            for (i in 25 until 27){
                for(j in 24 until MAP_WIDTH)
                    matrix[i][j] = PARED
            }
            //Contorno Paredes
            for (i in 15 until 18)
                matrix[i][11] = PARED
            for (i in 22 until 25)
                matrix[i][11] = PARED
            for (i in 15 until 18)
                matrix[i][25] = PARED
            for (i in 22 until 25)
                matrix[i][25] = PARED
            //Escaleras arriba izquierda
            matrix[17][10] = PARED
            matrix[17][9] = PARED
            matrix[17][8] = PARED
            matrix[17][7] = PARED
            matrix[17][5] = PARED
            matrix[17][4] = PARED
            matrix[17][3] = PARED
            matrix[17][2] = PARED
            matrix[17][1] = PARED
            //Escaleras abajo
            matrix[22][10] = PARED
            matrix[22][9] = PARED
            matrix[22][8] = PARED
            matrix[22][7] = PARED
            matrix[22][5] = PARED
            matrix[22][4] = PARED
            matrix[22][3] = PARED
            matrix[22][2] = PARED
            matrix[22][1] = PARED
            //Escaleras arriba derecha
            matrix[17][26] = PARED
            matrix[17][27] = PARED
            matrix[17][28] = PARED
            matrix[17][30] = PARED
            matrix[17][31] = PARED
            matrix[17][32] = PARED
            matrix[17][33] = PARED
            //Escaleras abajo
            matrix[22][26] = PARED
            matrix[22][27] = PARED
            matrix[22][28] = PARED
            matrix[22][30] = PARED
            matrix[22][31] = PARED
            matrix[22][32] = PARED
            matrix[22][33] = PARED


            for (i in 3 until 8){
                for(j in 20 until 26)
                    matrix[i][j] = PARED
            }

            altura = 8
            for(j in 20 until MAP_WIDTH)
                matrix[altura][j] = PARED

            altura = 9
            for(j in 21 until MAP_WIDTH)
                matrix[altura][j] = PARED

            for (i in 10 until 13){
                for(j in 22 until 26)
                    matrix[i][j] = PARED
            }

            var ancho = 13
            for(i in 8 until 14)
                matrix[i][ancho] = PARED
            ancho = 14
            for(i in 9 until 13)
                matrix[i][ancho] = PARED

            ancho = 12
            for(i in 26 until 31)
                matrix[i][ancho] = PARED
            ancho = 13
            for(i in 27 until 30)
                matrix[i][ancho] = PARED


            //Taquilla arriba
            matrix[11][21] = INTERACTIVE
            matrix[12][21] = INTERACTIVE

            //Torniquetes izquierda
            matrix[18][10] = INTERACTIVE
            matrix[19][10] = PARED
            matrix[20][10] = INTERACTIVE
            matrix[21][10] = INTERACTIVE

            //Torniquetes derecha
            matrix[18][26] = INTERACTIVE
            matrix[19][26] = PARED
            matrix[20][26] = INTERACTIVE
            matrix[21][26] = INTERACTIVE
            //Taquillas
            matrix[27][31] = INTERACTIVE
            matrix[28][31] = INTERACTIVE
            matrix[27][32] = PARED
            matrix[28][32] = PARED
            //Máquinas abajo
            matrix[27][14] = INTERACTIVE
            matrix[28][14] = INTERACTIVE
            //Puestos
            matrix[21][12] = INTERACTIVE
            matrix[20][32] = INTERACTIVE
            //Escaleras
            matrix[18][6] = INTERACTIVE
            matrix[21][6] = INTERACTIVE
            matrix[21][29] = INTERACTIVE
            matrix[18][29] = INTERACTIVE
            //Mural
            matrix[19][1] = INTERACTIVE
            //Mapa del Metro
            matrix[24][12] = INTERACTIVE

            matrix[27][33] = PARED
            matrix[28][33] = PARED

            return matrix
        }


        private fun createAndenesMetroPolitecnicoMatrix(): Array<Array<Int>> {
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

            matrix[10][20] = INTERACTIVE
            matrix[26][20] = INTERACTIVE
            // Pared al 60% de la altura desde arriba (equivale a 30% desde abajo)
            val alturaPared = (MAP_HEIGHT * 0.6).toInt() // 28 en un mapa 40x40
            for (j in 0 until MAP_WIDTH) {
                matrix[alturaPared][j] = PARED
            }
            // Pared al 40% de la altura desde arriba (equivale a 30% desde abajo)
            val alturaPared1 = (MAP_HEIGHT * 0.33).toInt() // 28 en un mapa 40x40
            for (j in 0 until MAP_WIDTH) {
                matrix[alturaPared1][j] = PARED
            }

            val ancho = 6
            for(i in 4 until 36)
                matrix[ancho][i] = PARED

            val ancho1 = 32
            for(i in 4 until 36)
                matrix[ancho1][i] = PARED

            return matrix
        }


        private fun createRedMetroMatrix(): Array<Array<Int>> {
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
            val ancho2 = 3
            for(i in 1 until 39)
                matrix[ancho2][i] = PARED
            val ancho = 2
            for(i in 1 until 39)
                matrix[ancho][i] = PARED
            val ancho1 = 1
            for(i in 1 until 39)
                matrix[ancho1][i] = PARED
            //Línea 5
            matrix[9][14] = INTERACTIVE
            matrix[11][15] = INTERACTIVE
            matrix[13][15] = INTERACTIVE
            matrix[14][16] = INTERACTIVE
            matrix[15][17] = INTERACTIVE
            matrix[16][19] = INTERACTIVE
            matrix[16][20] = INTERACTIVE
            matrix[17][22] = INTERACTIVE
            matrix[16][24] = INTERACTIVE
            matrix[18][25] = INTERACTIVE
            matrix[19][25] = INTERACTIVE
            matrix[21][25] = INTERACTIVE
            matrix[22][28] = INTERACTIVE
            //Línea 6
            matrix[9][5] = INTERACTIVE
            matrix[10][6] = INTERACTIVE
            matrix[11][7] = INTERACTIVE
            matrix[11][9] = INTERACTIVE
            matrix[11][11] = INTERACTIVE
            matrix[11][13] = INTERACTIVE
            matrix[11][16] = INTERACTIVE
            matrix[12][18] = INTERACTIVE
            matrix[12][20] = INTERACTIVE
            matrix[12][22] = INTERACTIVE
            //Línea 4
            matrix[13][21] = INTERACTIVE
            matrix[14][20] = INTERACTIVE
            matrix[17][20] = INTERACTIVE
            matrix[18][19] = INTERACTIVE
            matrix[20][19] = INTERACTIVE
            matrix[21][19] = INTERACTIVE
            matrix[22][18] = INTERACTIVE
            matrix[23][19] = INTERACTIVE
            //Línea 7 naranja
            matrix[11][5] = INTERACTIVE
            matrix[12][6] = INTERACTIVE
            matrix[14][6] = INTERACTIVE
            matrix[15][6] = INTERACTIVE
            matrix[17][6] = INTERACTIVE
            matrix[19][6] = INTERACTIVE
            matrix[20][6] = INTERACTIVE
            matrix[22][6] = INTERACTIVE
            matrix[23][7] = INTERACTIVE
            matrix[25][7] = INTERACTIVE
            matrix[26][7] = INTERACTIVE
            matrix[28][6] = INTERACTIVE
            matrix[29][6] = INTERACTIVE
            //Línea 12
            matrix[28][8] = INTERACTIVE
            matrix[28][9] = INTERACTIVE
            matrix[28][11] = INTERACTIVE
            matrix[29][12] = INTERACTIVE
            matrix[30][13] = INTERACTIVE
            matrix[30][15] = INTERACTIVE
            matrix[30][19] = INTERACTIVE
            matrix[30][22] = INTERACTIVE
            matrix[32][21] = INTERACTIVE
            matrix[33][21] = INTERACTIVE
            matrix[35][22] = INTERACTIVE
            matrix[35][24] = INTERACTIVE
            matrix[35][25] = INTERACTIVE
            matrix[36][27] = INTERACTIVE
            matrix[36][29] = INTERACTIVE
            matrix[36][30] = INTERACTIVE
            matrix[37][32] = INTERACTIVE
            matrix[37][33] = INTERACTIVE
            matrix[38][34] = INTERACTIVE
            //Línea A
            matrix[23][28] = INTERACTIVE
            matrix[24][30] = INTERACTIVE
            matrix[25][32] = INTERACTIVE
            matrix[27][33] = INTERACTIVE
            matrix[28][33] = INTERACTIVE
            matrix[30][34] = INTERACTIVE
            matrix[31][34] = INTERACTIVE
            matrix[32][35] = INTERACTIVE
            matrix[33][36] = INTERACTIVE
            //Línea 3
            matrix[10][19] = INTERACTIVE
            matrix[13][17] = INTERACTIVE
            matrix[16][14] = INTERACTIVE
            matrix[17][14] = INTERACTIVE
            matrix[18][14] = INTERACTIVE
            matrix[19][14] = INTERACTIVE
            matrix[20][14] = INTERACTIVE
            matrix[21][13] = INTERACTIVE
            matrix[22][13] = INTERACTIVE
            matrix[23][13] = INTERACTIVE
            matrix[25][12] = INTERACTIVE
            matrix[26][12] = INTERACTIVE
            matrix[27][12] = INTERACTIVE
            matrix[30][10] = INTERACTIVE
            matrix[31][9] = INTERACTIVE
            matrix[32][8] = INTERACTIVE
            matrix[33][9] = INTERACTIVE
            matrix[35][9] = INTERACTIVE
            //Línea B
            matrix[4][36] = INTERACTIVE
            matrix[5][35] = INTERACTIVE
            matrix[6][34] = INTERACTIVE
            matrix[8][34] = INTERACTIVE
            matrix[9][33] = INTERACTIVE
            matrix[11][32] = INTERACTIVE
            matrix[12][31] = INTERACTIVE
            matrix[14][31] = INTERACTIVE
            matrix[15][30] = INTERACTIVE
            matrix[16][28] = INTERACTIVE
            matrix[17][26] = INTERACTIVE
            matrix[18][23] = INTERACTIVE
            matrix[19][22] = INTERACTIVE
            matrix[20][20] = INTERACTIVE
            matrix[18][18] = INTERACTIVE
            matrix[18][17] = INTERACTIVE
            matrix[17][15] = INTERACTIVE
            matrix[17][13] = INTERACTIVE
            //Línea 9
            matrix[23][26] = INTERACTIVE
            matrix[23][25] = INTERACTIVE
            matrix[23][22] = INTERACTIVE
            matrix[23][20] = INTERACTIVE
            matrix[23][16] = INTERACTIVE
            matrix[23][14] = INTERACTIVE
            matrix[23][10] = INTERACTIVE
            matrix[23][8] = INTERACTIVE
            //Línea 2
            matrix[15][2] = INTERACTIVE
            matrix[15][4] = INTERACTIVE
            matrix[16][8] = INTERACTIVE
            matrix[16][9] = INTERACTIVE
            matrix[17][10] = INTERACTIVE
            matrix[18][11] = INTERACTIVE
            matrix[18][12] = INTERACTIVE
            matrix[18][13] = INTERACTIVE
            matrix[18][15] = INTERACTIVE
            matrix[19][16] = INTERACTIVE
            matrix[19][17] = INTERACTIVE
            matrix[20][17] = INTERACTIVE
            matrix[22][16] = INTERACTIVE
            matrix[24][16] = INTERACTIVE
            matrix[25][16] = INTERACTIVE
            matrix[26][16] = INTERACTIVE
            matrix[27][15] = INTERACTIVE
            matrix[28][15] = INTERACTIVE
            matrix[31][14] = INTERACTIVE
            matrix[32][15] = INTERACTIVE
            //Línea 8
            matrix[19][15] = INTERACTIVE
            matrix[20][15] = INTERACTIVE
            matrix[21][15] = INTERACTIVE
            matrix[22][15] = INTERACTIVE
            matrix[23][18] = INTERACTIVE
            matrix[24][20] = INTERACTIVE
            matrix[26][20] = INTERACTIVE
            matrix[27][21] = INTERACTIVE
            matrix[28][21] = INTERACTIVE
            matrix[29][21] = INTERACTIVE
            matrix[30][24] = INTERACTIVE
            matrix[31][25] = INTERACTIVE
            matrix[31][27] = INTERACTIVE
            matrix[32][29] = INTERACTIVE
            //Línea 1
            matrix[22][26] = INTERACTIVE
            matrix[22][24] = INTERACTIVE
            matrix[21][23] = INTERACTIVE
            matrix[21][22] = INTERACTIVE
            matrix[20][21] = INTERACTIVE
            matrix[20][18] = INTERACTIVE
            matrix[20][16] = INTERACTIVE
            matrix[20][13] = INTERACTIVE
            matrix[21][11] = INTERACTIVE
            matrix[21][10] = INTERACTIVE
            matrix[21][8] = INTERACTIVE
            matrix[22][8] = INTERACTIVE
            matrix[24][4] = INTERACTIVE

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

        private fun createEsimeMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // ========== BORDES EXTERIORES ==========
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    // Bordes exteriores
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // ========== EDIFICIOS BLOQUEADOS - SINCRONIZADO CON Esime.kt ==========
            // Basado exactamente en las definiciones de collisionAreas en Esime.kt

            // Edificio 1 - Rectángulos bloqueados
            // Rect(7, 28, 14, 29) - Rectángulo grande desde entrada del Edificio 1
            for (i in 7..14) {
                for (j in 28..29) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(16, 28, 17, 29) - Cuadrado que deja pasillo
            for (i in 16..17) {
                for (j in 28..29) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(7, 31, 14, 32) - Parte inferior
            for (i in 7..14) {
                for (j in 31..32) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Edificio 2 - Rectángulos bloqueados
            // Rect(7, 22, 14, 23) - Rectángulo grande desde entrada del Edificio 2
            for (i in 7..14) {
                for (j in 22..23) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(16, 22, 17, 23) - Cuadrado que deja pasillo
            for (i in 16..17) {
                for (j in 22..23) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(7, 25, 14, 26) - Parte inferior
            for (i in 7..14) {
                for (j in 25..26) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Edificio 3 - Solo bloquear área derecha, dejar entrada libre frontal
            // Rect(7, 15, 14, 16) - Área derecha bloqueada del Edificio 3
            for (i in 7..14) {
                for (j in 15..16) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(16, 15, 17, 16) - Cuadrado que deja pasillo
            for (i in 16..17) {
                for (j in 15..16) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(7, 18, 14, 19) - Parte inferior
            for (i in 7..14) {
                for (j in 18..19) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Edificio 4 - Rectángulos bloqueados
            // Rect(7, 9, 14, 10) - Rectángulo grande desde entrada del Edificio 4
            for (i in 7..14) {
                for (j in 9..10) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(16, 9, 17, 10) - Cuadrado que deja pasillo
            for (i in 16..17) {
                for (j in 9..10) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(7, 12, 14, 13) - Parte inferior
            for (i in 7..14) {
                for (j in 12..13) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Edificio 5 - Rectángulos bloqueados
            // Rect(7, 3, 14, 4) - Rectángulo grande desde entrada del Edificio 5
            for (i in 7..14) {
                for (j in 3..4) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(16, 3, 17, 4) - Cuadrado que deja pasillo
            for (i in 16..17) {
                for (j in 3..4) {
                    matrix[j][i] = INACCESSIBLE
                }
            }
            // Rect(7, 6, 14, 7) - Parte superior edificio 5
            for (i in 7..14) {
                for (j in 6..7) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // ========== ÁREAS ADICIONALES BLOQUEADAS ==========
            // Pastos - Rect(7, 34, 38, 38)
            for (i in 7..38) {
                for (j in 34..38) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Pastos laterales - Rect(32, 29, 38, 38)
            for (i in 32..38) {
                for (j in 29..38) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Área central bloqueada - Rect(24, 6, 29, 18)
            for (i in 24..29) {
                for (j in 6..18) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // Zona inaccesible superior - Rect(7, 1, 38, 4)
            for (i in 7..38) {
                for (j in 1..4) {
                    matrix[j][i] = INACCESSIBLE
                }
            }

            // ========== PUNTOS INTERACTIVOS ==========
            // Entrada al Edificio 3 (accesible) - Posición de entrada frontal libre
            matrix[17][8] = INTERACTIVE // Entrada Edificio 3

            // Punto de transición ESIME a Zacatenco (basado en ESIME_TO_ZACATENCO_POSITION)
            matrix[ESIME_TO_ZACATENCO_POSITION.second][ESIME_TO_ZACATENCO_POSITION.first] = INTERACTIVE

            // Punto de entrada desde Zacatenco (basado en las definiciones de transición)
            matrix[2][38] = INTERACTIVE // Entrada desde Zacatenco

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

        private fun createPlazaTorresN1Matrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } }

            // Área jugable (un rectángulo)
            val startX = 10
            val startY = 15
            val roomWidth = 20
            val roomHeight = 10

            for (y in startY until startY + roomHeight) {
                for (x in startX until startX + roomWidth) {
                    matrix[y][x] = PATH
                }
            }

            // Punto de salida para regresar a la planta baja
            matrix[startY + roomHeight - 1][startX + roomWidth / 2] = INTERACTIVE // Salida

            return matrix
        }

        /**
         * Matriz para el Laboratorio de Posgrado.
         * Mapa ASCII Art:
         * +-------------------------------------------------------------------------+
         * |                          Laboratorio de Posgrado                          |
         * |                                                                         |
         * |   +----+  +----+  +----+  +----+  +----+                                |
         * |   | 💻 |  | 💻 |  | 💻 |  | 💻 |  | 💻 |                                |
         * |   +----+  +----+  +----+  +----+  +----+        +----------------+      |
         * |                                                 |   Profesor's   |      |
         * |   +----+  +----+  +----+  +----+  +----+        |      Desk      |      |
         * |   | 💻 |  | 💻 |  | 💻 |  | 💻 |  | 💻 |        +----------------+      |
         * |   +----+  +----+  +----+  +----+  +----+                                |
         * |                                                   +----------------+    |
         * |   +----+  +----+  +----+  +----+  +----+          |                |    |
         * |   | 💻 |  | 💻 |  | 💻 |  | 💻 |  | 💻 |          |   Proyector    |    |
         * |   +----+  +----+  +----+  +----+  +----+          |                |    |
         * |                                                   +----------------+    |
         * |   +----+  +----+  +----+  +----+  +----+                                |
         * |   | 💻 |  | 💻 |  | 💻 |  | 💻 |  | 💻 |                                |
         * |   +----+  +----+  +----+  +----+  +----+                                |
         * |                                                                         |
         * |   🚪 Salida                                                              |
         * +-------------------------------------------------------------------------+
         * Representa un laboratorio de cómputo con un proyector a la derecha.
         */
        private fun createLabPosgradoMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Bordes exteriores del laboratorio
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // Pantalla del proyector en la pared derecha (inaccesible)
            val projectorScreenStart = MAP_HEIGHT / 2 - 5
            val projectorScreenEnd = MAP_HEIGHT / 2 + 5
            for (i in projectorScreenStart..projectorScreenEnd) {
                matrix[i][MAP_WIDTH - 2] = INACCESSIBLE
            }

            // Mesa del profesor en la parte frontal (derecha, cerca del proyector)
            for (i in projectorScreenStart - 4 until projectorScreenStart) {
                for (j in MAP_WIDTH - 15 until MAP_WIDTH - 8) {
                    matrix[i][j] = INACCESSIBLE
                }
            }

            // Filas de computadoras en una cuadrícula
            // 4 filas de computadoras
            for (row in 0..3) {
                val rowY = 8 + (row * 7) // Separación vertical entre filas

                // 5 estaciones de cómputo por fila
                for (station in 0..4) {
                    val stationX = 3 + (station * 4) // Separación horizontal

                    // Cada estación es un bloque de 2x2
                    for (i in rowY..rowY + 1) {
                        for (j in stationX..stationX + 1) {
                            if (i < MAP_HEIGHT && j < MAP_WIDTH) {
                                matrix[i][j] = INACCESSIBLE
                            }
                        }
                    }
                }
            }

            // Puerta de entrada/salida en la parte inferior izquierda
            matrix[MAP_HEIGHT - 4][4] = INTERACTIVE

            return matrix
        }

        /**
         * NUEVO MAPA: Plaza Torres
         */
        private fun createPlazaTorresMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // Walmart (Área grande azul en la parte inferior izquierda)
            for (i in 20 until 39) { for (j in 1 until 22) { matrix[i][j] = INACCESSIBLE } }
            // Suburbia (Área grande rosa en la parte superior)
            for (i in 1 until 16) { for (j in 14 until 36) { matrix[i][j] = INACCESSIBLE } }
            // Smart Fit (Área azul en la esquina superior izquierda)
            for (i in 1 until 11) { for (j in 1 until 12) { matrix[i][j] = INACCESSIBLE } }
            // Zona de restaurantes (Vips, Burger King, etc. - Área verde a la derecha)
            for (i in 1 until 18) { for (j in 37 until 39) { matrix[i][j] = INACCESSIBLE } }
            for (i in 12 until 18) { for (j in 32 until 37) { matrix[i][j] = INACCESSIBLE } }

            // Pasillo horizontal principal (debajo de Suburbia)
            for (i in 16 until 22) { for (j in 10 until 38) { matrix[i][j] = PATH } }
            // Pasillo vertical principal (entre Walmart y las tiendas pequeñas)
            for (i in 16 until 38) { for (j in 22 until 28) { matrix[i][j] = PATH } }
            // Pasillo secundario (hacia Smart Fit y Citibanamex)
            for (i in 11 until 16) { for (j in 8 until 14) { matrix[i][j] = PATH } }
            for (i in 11 until 20) { for (j in 11 until 14) { matrix[i][j] = PATH } }

            // SALIDA A ZACATENCO
            matrix[14][10] = INTERACTIVE // Punto azul cerca de Smart Fit
            matrix[20][31] = INTERACTIVE // Punto azul en el pasillo central
            matrix[37][24] = INTERACTIVE // Punto azul en la salida cerca de Walmart

            // Kioscos o islas
            matrix[18][15] = INACCESSIBLE
            matrix[18][30] = INACCESSIBLE

            matrix[18][25] = INTERACTIVE // Punto para subir al nivel del Cinepolis

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

            // Wall de (3,10) a (5,10) y (3,11) a (4,11)
            for (j in 3..5) matrix[10][j] = WALL
            for (j in 3..4) matrix[11][j] = WALL

            // ✅ QUITAR walls específicos - forzar PATH en (6,8) a (11,8) y (6,9) a (11,9)
            for (j in 6..11) {
                matrix[8][j] = PATH  // Fila Y=8 del X=6 al X=11
                matrix[9][j] = PATH  // Fila Y=9 del X=6 al X=11
            }

            // Zonas bloqueadas principales - optimizadas
            val blockedZones = arrayOf(
                // formato: arrayOf(startI, endI, startJ, endJ)
                arrayOf(3, 12, 20, 37), // Zona superior derecha
                arrayOf(3, 8, 15, 37),  // Extensión superior derecha
                arrayOf(15, 25, 28, 37), // Zona media derecha
                arrayOf(18, 22, 25, 28), // Extensión zona verde
                arrayOf(8, 15, 25, 37),  // Área verde específica
                arrayOf(12, 15, 20, 25), // Áreas adicionales 12-14
                arrayOf(15, 16, 21, 28), // Línea Y=15
                arrayOf(16, 17, 22, 28), // Línea Y=16
                arrayOf(17, 18, 23, 28), // Línea Y=17
                arrayOf(8, 9, 16, 22),   // Línea Y=8 (pero se sobrescribe después)
                arrayOf(9, 10, 17, 21),  // Línea Y=9 (pero se sobrescribe después)
                arrayOf(10, 11, 18, 20), // Línea Y=10
                arrayOf(3, 10, 3, 12),   // Figura negra superior izquierda
                arrayOf(3, 7, 12, 20),   // Figura negra superior derecha
                arrayOf(3, 8, 8, 30)     // Rectángulo superior final
            )

            // Aplicar todas las zonas bloqueadas
            blockedZones.forEach { zone ->
                for (i in zone[0] until zone[1]) {
                    for (j in zone[2] until zone[3]) {
                        // Aplicar condiciones especiales para ciertas zonas
                        when {
                            zone[0] == 3 && zone[2] == 20 -> { // Zona diagonal superior
                                val diagonal = (i - 3) + (j - 20)
                                if (diagonal > 6) matrix[i][j] = WALL
                            }
                            zone[0] == 25 && zone[2] == 30 -> { // Zona diagonal inferior
                                val diagonal = (37 - i) + (j - 30)
                                if (diagonal > 8) matrix[i][j] = WALL
                            }
                            else -> matrix[i][j] = WALL
                        }
                    }
                }
            }

            // Puntos específicos bloqueados
            val specificPoints = arrayOf(
                arrayOf(25, 29), arrayOf(22, 27), arrayOf(18, 24), arrayOf(11, 19)
            )
            specificPoints.forEach { point ->
                matrix[point[0]][point[1]] = WALL
            }

            // Zona diagonal inferior derecha
            for (i in 25 until 37) {
                for (j in 30 until 37) {
                    val diagonal = (37 - i) + (j - 30)
                    if (diagonal > 8) matrix[i][j] = WALL
                }
            }

            // ✅ ASEGURAR que las zonas liberadas sigan siendo PATH (después de aplicar todas las zonas)
            for (j in 6..11) {
                matrix[8][j] = PATH  // Fuerza PATH en Y=8 del X=6 al X=11
                matrix[9][j] = PATH  // Fuerza PATH en Y=9 del X=6 al X=11
            }

            for ( i in 6..23){
                matrix[28][i]= WALL
            }
            for ( i in 9..20){
                for (j in 31..33){
                    if (i == 14 ){
                        matrix[j][i] = PATH
                    }else{
                        matrix[j][i] = WALL
                    }
                }
            }

            for ( i in 7..22){
                if(i in 14  .. 15){
                    matrix[25][i]= PATH
                }
                matrix[25][i]= WALL
            }

            for ( i in 9..11){
                for (j in 20..23){
                    matrix[j][i] = WALL
                }
            }

            for ( i in 13..15){
                for (j in 21..22){
                    matrix[j][i] = WALL
                }
            }

            for (i in 25..26){
                matrix[28][i]= WALL
            }

            for (j in 30..31){
                matrix[j][24]= WALL
            }

            for (j in 30..32){
                matrix[j][5]= WALL
            }

            // Puntos interactivos
            matrix[35][25] = INTERACTIVE // Salida a Zacatenco
            matrix[31][6] = INTERACTIVE  // Entrada a biblioteca
            matrix[28][19] = INTERACTIVE  // Entrada a edificio

            return matrix
        }
        private fun createBibliotecaESIAMatrix(): Array<Array<Int>> {
            // Crear matriz de 40x40 con TODO como PATH (valor 2)
            val matrix = Array(40) { Array(40) { 2 } }

            // ✅ NO BLOQUEAR NADA - Solo bordes mínimos
            // Borde superior
            for (j in 0..39) {
                matrix[0][j] = 1  // WALL
            }
            // Borde inferior
            for (j in 0..39) {
                matrix[39][j] = WALL // WALL
            }
            // Borde izquierdo
            for (i in 1..38) {
                matrix[i][0] = 1  // WALL
            }
            // Borde derecho
            for (i in 1..38) {
                matrix[i][39] = WALL  // WALL
            }

            for (i in 3..5) {
                for (j in 3 until 11) {
                    matrix[i][j] = WALL
                }
            }

            for (i in 9..11) {
                for (j in 3 until 11) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (21,15) a (27,15)
            for (i in 17.. 19) {
                for (j in 3 until 11) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (22,16) a (27,16)
            for (i in 25 .. 26) {
                for (j in 3 until 11) {
                    matrix[i][j] = WALL
                }
            }

            // Bloquear área adicional (23,17) a (27,17)
            for (i in 32.. 34) {
                for (j in 3 until 11) {
                    matrix[i][j] = WALL
                }
            }

            for (i in 27..34) {
                for (j in 18 ..19) {
                    matrix[i][j] = WALL
                }
            }

            // NUEVO: Bloquear área (17,9) a (20,9)
            for (i in 15.. 22) {
                for (j in 16 until 21) {
                    matrix[i][j] = WALL
                }
            }

            for (i in 4..11) {
                for (j in 17 ..19) {
                    matrix[i][j] = WALL
                }
            }
            for (i in 3..5) {
                for (j in 27 ..34) {
                    matrix[i][j] = WALL
                }
            }
            for (i in 32..34) {
                for (j in 26 ..33) {
                    matrix[i][j] = WALL
                }
            }
            for (j in 26..27){
                matrix[28][j] = WALL
            }
            for (j in 33..34){
                matrix[28][j] = WALL
            }
            for (j in 25..26){
                matrix[24][j] = WALL
            }
            for (j in 32..33){
                matrix[24][j] = WALL
            }

            for (j in 33..34){
                matrix[13][j] = WALL
            }
            for (j in 26..27){
                matrix[13][j] = WALL
            }
            for (j in 26..27){
                matrix[9][j] = WALL
            }
            for (j in 33..34){
                matrix[9][j] = WALL
            }

            // Punto de entrada/salida
            matrix[19][38] = 0  // INTERACTIVE

            // ✅ LOGS para verificar
            Log.d("BibliotecaMatrix", "Posición [5][9] = ${matrix[5][9]} (debería ser 2)")
            Log.d("BibliotecaMatrix", "Posición [8][9] = ${matrix[8][9]} (debería ser 2)")
            Log.d("BibliotecaMatrix", "Posición [15][15] = ${matrix[15][15]} (debería ser 2)")

            return matrix
        }
        private fun createEdificioESIAMatrix(): Array<Array<Int>> {
            // Usa las constantes ya definidas en tu proyecto (PATH, INTERACTIVE, MAP_HEIGHT, MAP_WIDTH)
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            for (i in 0..39) {
                for (j in 1 until 7) {
                    matrix[j][i] = WALL
                }
            }

            for (i in 38.. 39) {
                for (j in 7 until 27) {
                    matrix[j][i] = WALL
                }
            }

            for (j in 7 until 27) {
                matrix[j][0] = WALL
            }


            // Entrada del edificio en (19,28) -> matrix[y][x]
            matrix[34][20] = INTERACTIVE
            matrix[28][4] = INTERACTIVE
            matrix[28][9] = INTERACTIVE
            matrix[28][14] = INTERACTIVE
            matrix[28][18] = INTERACTIVE
            matrix[28][23] = INTERACTIVE
            matrix[28][28] = INTERACTIVE
            matrix[28][33] = INTERACTIVE
            matrix[28][37] = INTERACTIVE

            // DEBUG logs para confirmar que la función se ejecuta y el tile está correcto
            Log.d("MapMatrixProvider", "createEdificioESIAMatrix: MAP_HEIGHT=$MAP_HEIGHT MAP_WIDTH=$MAP_WIDTH")
            Log.d("MapMatrixProvider", "createEdificioESIAMatrix: matrix[28][19]=${matrix[28][19]} (INTERACTIVE=$INTERACTIVE, PATH=$PATH)")

            return matrix
        }
        // En MapMatrixProvider.kt, agregar este método:

        private fun getSalonESIAMatrix(): Array<Array<Int>> {
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { WALL } }

            // Dimensiones del aula
            val roomWidth = 40
            val roomHeight = 40
            val startX = 0
            val startY = 0

            // Interior del salón (espacio abierto)
            for (i in startY until startY + roomHeight) {
                for (j in startX until startX + roomWidth) {
                    matrix[i][j] = PATH
                }
            }

            // Puerta de salida hacia el edificio ESIA (lado izquierdo)
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

            for (i in 4..32 step 4) {
                matrix[15][i] = INACCESSIBLE
            }
            for (i in 4..32 step 4) {
                matrix[20][i] = INACCESSIBLE
            }
            for (i in 4..32 step 4) {
                matrix[25][i] = INACCESSIBLE
            }
            for (i in 4..32 step 4) {
                matrix[29][i] = INACCESSIBLE
            }
            for (i in 4..32 step 4) {
                matrix[34][i] = INACCESSIBLE
            }

            return matrix
        }



        /**
         * Comprueba si la coordenada especificada es un punto de transición entre mapas
         */
        fun isMapTransitionPoint(mapId: String, x: Int, y: Int): String? {
            // Imprimimos para depuración
            Log.d("MapTransition", "Checking transition at $mapId: ($x, $y)")

            // Para el edificio 2
            if (mapId == MAP_BUILDING2) {
                // Compara la posición actual del jugador (x, y) con las coordenadas interactivas.
                when (Pair(x, y)) {
                    // --- Transiciones a Salones ---
                    Pair(2, 22) -> return MAP_SALON2001
                    Pair(7, 22) -> return MAP_SALON2002
                    Pair(12, 22) -> return MAP_SALON2003
                    Pair(16, 22) -> return MAP_SALON2004
                    Pair(20, 22) -> return MAP_SALON2005
                    Pair(25, 22) -> return MAP_SALON2006
                    // Pair(35, 22) -> return MAP_WC // Puedes activar esta línea si los baños son un mapa separado

                    // --- Transición a otros pisos (Escaleras) ---
                    Pair(17, 21) -> {
                        Log.d("MapTransition", "Transition to Building 2, Floor 1 triggered!")
                        return MAP_BUILDING2_PISO1
                    }

                    // --- Transición para salir del edificio ---
                    Pair(1, 24) -> {
                        Log.d("MapTransition", "Transition to Main Map triggered!")
                        return MAP_MAIN
                    }
                }
            }

            if (mapId == MAP_BUILDING2_PISO1) {
                // Compara la posición actual del jugador (x, y) con las coordenadas interactivas.
                when (Pair(x, y)) {
                    // --- Transiciones a Salones ---
                    Pair(2, 22) -> return MAP_SALON2101
                    Pair(7, 22) -> return MAP_SALON2102
                    Pair(12, 22) -> return MAP_SALON2103
                    Pair(16, 22) -> return MAP_SALON2104
                    Pair(20, 22) -> return MAP_SALON2105
                    Pair(25, 22) -> return MAP_SALON2106
                    // Pair(35, 22) -> return MAP_WC // Puedes activar esta línea si los baños son un mapa separado

                    // --- Transición a otros pisos (Escaleras) ---
                    Pair(21, 17) -> {
                        Log.d("MapTransition", "Transition to Building 2, Floor 1 triggered!")
                        return MAP_BUILDING2
                    }

                    Pair(18, 17) -> {
                        Log.d("MapTransition", "Transition to Building 2, Floor 1 triggered!")
                        return MAP_BUILDING2_PISO2
                    }
               }
            }

            if (mapId == MAP_BUILDING2_PISO2) {
                // Compara la posición actual del jugador (x, y) con las coordenadas interactivas.
                when (Pair(x, y)) {
                    // --- Transiciones a Salones ---
                    Pair(2, 22) -> return MAP_SALON2201
                    Pair(7, 22) -> return MAP_SALON2202
                    Pair(12, 22) -> return MAP_SALON2203
                    Pair(16, 22) -> return MAP_SALON2204
                    Pair(20, 22) -> return MAP_SALON2205
                    Pair(25, 22) -> return MAP_SALON2206
                    // Pair(35, 22) -> return MAP_WC // Puedes activar esta línea si los baños son un mapa separado

                    // --- Transición a otros pisos (Escaleras) ---
                    Pair(21, 17) -> {
                        Log.d("MapTransition", "Transition to Building 2, Floor 1 triggered!")
                        return MAP_BUILDING2_PISO1
                    }
                }
            }
            // Para el edificio 2
            // Transiciones desde cada salon al edificio
            if (mapId == MAP_SALON2001 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2002 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2003 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2004 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2005 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2006 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }

            if (mapId == MAP_SALON2101 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2102 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2103 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2104 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2105 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2106 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }

            if (mapId == MAP_SALON2201 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2202 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2203 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2204 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2205 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2206 && x == 0 && y == 6) {
                return MAP_BUILDING2
            }
            if (mapId == MAP_SALON2009 && x == 0 && y == 6) {
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

            if (mapId == MAP_BUILDING2 && x == 17 && y == 15) {
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
            if (mapId == MAP_MAIN && x == 33 && y == 28) {
                return MAP_CANCHA_IA
            }
            //Retorno al mapa principal desde cancha de IA
            if (mapId == MAP_CANCHA_IA) {
                // Rango ampliado: detectamos si está en las primeras 3 o últimas 3 filas/columnas
                val isLeftExit = x <= 3
                val isRightExit = x >= 36
                val isTopExit = y <= 3
                val isBottomExit = y >= 36

                if (isLeftExit || isRightExit || isTopExit || isBottomExit) {
                    // Imprimir log para asegurar que entra aquí
                    Log.d("MapTransition", "Saliendo de CanchaIA por borde: $x, $y")
                    return MAP_MAIN
                }
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

            // Transición DESDE Zacatenco HACIA Plaza Torres
            if (mapId == MAP_ZACATENCO && x == 20 && y == 10) { // Coordenada del punto azul en Zacatenco
                return MAP_PLAZA_TORRES
            }

            // Transición DESDE Plaza Torres HACIA Zacatenco
            if (mapId == MAP_PLAZA_TORRES) {
                if ((x == 10 && y == 14) || // Cerca de Smart Fit
                    (x == 31 && y == 20) || // Pasillo central
                    (x == 24 && y == 37))   // Cerca de Walmart
                {
                    return MAP_ZACATENCO
                }

                // Transición DESDE Plaza Torres HACIA el nivel 1
                if (x == 25 && y == 18) {
                    return MAP_PLAZA_TORRES_N1
                }
            }

            // Transición DESDE el nivel 1 HACIA Plaza Torres
            if (mapId == MAP_PLAZA_TORRES_N1 && x == 20 && y == 24) {
                return MAP_PLAZA_TORRES
            }

            if (mapId == MAP_ESIA && x == 10 && y == 15) { // Ajusta las coordenadas según tu mapa
                return MAP_BIBLIOTECA_ESIA
            }

            // Transición DESDE Biblioteca ESIA hacia ESIA
            if (mapId == MAP_BIBLIOTECA_ESIA && x == 15 && y == 25) {
                return MAP_ESIA
            }

            return null
        }


        /**
         * Obtiene la posición inicial para un mapa destino
         */
        fun getInitialPositionForMap(mapId: String): Pair<Int, Int> {
            return when (mapId) {
                MAP_MAIN -> Pair(15, 15)  // Posición central en el mapa principal
                MAP_BUILDING4_F2 -> Pair(20, 16)  // Centro del pasillo principal del edificio 4
                MAP_BUILDING2 -> Pair(20, 16)
                MAP_BUILDING2_PISO1 -> Pair(20, 16)
                MAP_BUILDING2_PISO2 -> Pair(20, 16)
                MAP_SALON2001 -> Pair(20, 20)
                MAP_SALON2002 -> Pair(20, 20)
                MAP_SALON2003 -> Pair(20, 20)
                MAP_SALON2004 -> Pair(20, 20)
                MAP_SALON2005 -> Pair(20, 20)
                MAP_SALON2006 -> Pair(20, 20)
                MAP_SALON2101 -> Pair(20, 20)
                MAP_SALON2102 -> Pair(20, 20)
                MAP_SALON2103 -> Pair(20, 20)
                MAP_SALON2104 -> Pair(20, 20)
                MAP_SALON2105 -> Pair(20, 20)
                MAP_SALON2106 -> Pair(20, 20)
                MAP_SALON2201 -> Pair(20, 20)
                MAP_SALON2202 -> Pair(20, 20)
                MAP_SALON2203 -> Pair(20, 20)
                MAP_SALON2204 -> Pair(20, 20)
                MAP_SALON2205 -> Pair(20, 20)
                MAP_SALON2206 -> Pair(20, 20)

                MAP_SALON2009 -> Pair(20, 20)  // Posición central dentro del salón 2009
                MAP_SALON2010 -> Pair(20, 20)

                MAP_CAFETERIA -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_CABLEBUS -> Pair(2, 2) // Posicion central dentro del cablebus
                MAP_EDIFICIO_IA_BAJO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_EDIFICIO_IA_MEDIO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_EDIFICIO_IA_ALTO -> Pair(2, 2)  // Posición central dentro de la escomCAFE
                MAP_SALON4102 -> Pair(20, 35)  // Entrada del salón 4102
                MAP_PALAPAS_IA -> Pair(2, 2)

                MAP_PALAPAS_ISC -> Pair(38, 38) // Posición inicial dentro de palapas ISC
                MAP_EDIFICIO_GOBIERNO -> Pair(17, 5)  // Posición cerca de la entrada
                MAP_BIBLIOTECA -> Pair(17, 5)  // Posición cerca de la entrada
                MAP_PLAZA_TORRES -> Pair(18, 18) //Entrada ESCOM
                MAP_PLAZA_TORRES_N1 -> Pair(20, 16) //Entrada cinepolis plaza torres
                MAP_ESIA -> Pair(25, 35) // Posición inicial en ESIA (cerca de la entrada)
                MAP_BIBLIOTECA_ESIA -> Pair(15, 25)
                else -> Pair(MAP_WIDTH / 2, MAP_HEIGHT / 2)
            }
        }
        /**
         * Matriz para la Cancha de IA
         */
        private fun createCanchaIAMatrix(): Array<Array<Int>> {
            // 1. Mapa
            val matrix = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { PATH } }

            // 2. BORDES EXTERIORES
            for (i in 0 until MAP_HEIGHT) {
                for (j in 0 until MAP_WIDTH) {
                    if (i == 0 || i == MAP_HEIGHT - 1 || j == 0 || j == MAP_WIDTH - 1) {
                        matrix[i][j] = WALL
                    }
                }
            }

            // 3. PARED VERTICAL DIVISORIA (Cancha y Pasillo Gris)

            for (i in 4 until 37) {
                // Esta pared va desde la fila 5 hasta la 35.
                // Esto significa que las filas 1-4 (Naranja Arriba) están abiertas a la derecha.
                // Y las filas 36-39 (Naranja Abajo) están abiertas a la derecha.

                val espalapa = (i >= 17 && i <= 21)

                if (!espalapa) {
                    matrix[i][34] = WALL
                }
            }

            //Basketball Game
            matrix[14][13] = INTERACTIVE

            return matrix
        }
    }
}
