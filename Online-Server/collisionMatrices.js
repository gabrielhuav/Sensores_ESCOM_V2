// collisionMatrices.js
// Este archivo contiene todas las matrices de colisión para los diferentes mapas

// IMPORTANTE: Estos valores DEBEN coincidir con MapMatrixProvider.kt en Android:
// 0 = INTERACTIVE (caminable)
// 1 = WALL (no caminable)
// 2 = PATH (caminable - espacio libre)
// 3 = INACCESSIBLE (no caminable)

// Matriz de colisión para la cafetería (40x40)
const cafeteriaCollisionMatrix = Array(40).fill().map(() => Array(40).fill(2)); // TODO es PATH por defecto

// Inicializar los bordes del mapa como WALL
for (let i = 0; i < 40; i++) {
    cafeteriaCollisionMatrix[0][i] = 1; // Pared superior
    cafeteriaCollisionMatrix[39][i] = 1; // Pared inferior
    cafeteriaCollisionMatrix[i][0] = 1; // Pared izquierda
    cafeteriaCollisionMatrix[i][39] = 1; // Pared derecha
}

// COCINA (esquina superior izquierda)
for (let i = 2; i <= 8; i++) {
    for (let j = 2; j <= 15; j++) {
        if (i === 2 || i === 8 || j === 2 || j === 15) {
            cafeteriaCollisionMatrix[i][j] = 1; // Paredes de la cocina
        }
    }
}
// Mostrador de la cocina
for (let i = 4; i <= 6; i++) {
    for (let j = 4; j <= 13; j++) {
        cafeteriaCollisionMatrix[i][j] = 1;
    }
}

// MESAS/BANCAS (3 filas de 3 mesas cada una)
for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 3; col++) {
        const baseI = 12 + (row * 8);
        const baseJ = 10 + (col * 10);

        for (let i = baseI; i <= baseI+2; i++) {
            for (let j = baseJ; j <= baseJ+8; j++) {
                cafeteriaCollisionMatrix[i][j] = 1;
            }
        }
    }
}

// CAJA (parte inferior)
for (let i = 30; i <= 33; i++) {
    for (let j = 15; j <= 19; j++) {
        cafeteriaCollisionMatrix[i][j] = 1;
    }
}

// PUNTOS INTERACTIVOS (comida)
cafeteriaCollisionMatrix[12][8] = 0;  // Tacos 1
cafeteriaCollisionMatrix[12][32] = 0; // Tacos 2
cafeteriaCollisionMatrix[28][8] = 0;  // Burrito
cafeteriaCollisionMatrix[28][32] = 0; // Chile
cafeteriaCollisionMatrix[20][8] = 0;  // Guacamole

// ENTRADA
for (let i = 37; i <= 38; i++) {
    for (let j = 15; j <= 25; j++) {
        cafeteriaCollisionMatrix[i][j] = 0; // Zona interactiva de entrada/salida
    }
}

// Matriz de colisión para Edificio Gobierno (40x40) - CORREGIDA Y MEJORADA
const edificioGobiernoCollisionMatrix = Array(40).fill().map(() => Array(40).fill(2)); // Todo PATH por defecto

// Solo los bordes exteriores son WALL
for (let i = 0; i < 40; i++) {
    edificioGobiernoCollisionMatrix[i][0] = 1;
    edificioGobiernoCollisionMatrix[i][39] = 1;
}
for (let j = 0; j < 40; j++) {
    edificioGobiernoCollisionMatrix[0][j] = 1;
    edificioGobiernoCollisionMatrix[39][j] = 1;
}

// ========== CORRECCIONES CRÍTICAS ==========

// CORREGIDO: Crear pasillos principales PRIMERO
for (let i = 1; i < 39; i++) {
    // Pasillo horizontal superior (fila 5)
    if (i >= 1 && i <= 38) {
        edificioGobiernoCollisionMatrix[5][i] = 2;
    }
    // Pasillo horizontal inferior (fila 35)
    if (i >= 1 && i <= 38) {
        edificioGobiernoCollisionMatrix[35][i] = 2;
    }
}

// Pasillos verticales
for (let i = 1; i < 39; i++) {
    // Pasillo vertical izquierdo (columna 5)
    edificioGobiernoCollisionMatrix[i][5] = 2;
    // Pasillo vertical derecho (columna 35)
    edificioGobiernoCollisionMatrix[i][35] = 2;
}

// CORREGIDO: Auditorio, gestión y enfermeria - INACCESSIBLE COMPLETO
for (let i = 14; i <= 38; i++) {
    for (let j = 1; j <= 23; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesa 1 (arriba-izquierda) - INACCESSIBLE
for (let i = 2; i <= 4; i++) {
    for (let j = 2; j <= 4; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesa 2 (arriba-centro) - INACCESSIBLE
for (let i = 2; i <= 4; i++) {
    for (let j = 6; j <= 8; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesa 3 (arriba-derecha) - INACCESSIBLE
for (let i = 2; i <= 4; i++) {
    for (let j = 10; j <= 12; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesa 4 (abajo-izquierda) - INACCESSIBLE
for (let i = 9; i <= 11; i++) {
    for (let j = 2; j <= 4; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesa 5 (abajo-centro) - INACCESSIBLE
for (let i = 9; i <= 11; i++) {
    for (let j = 6; j <= 8; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesa 6 (abajo-derecha) - INACCESSIBLE
for (let i = 9; i <= 11; i++) {
    for (let j = 10; j <= 12; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Recepción - INACCESSIBLE
for (let i = 11; i <= 12; i++) {
    for (let j = 17; j <= 23; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mochilas - INACCESSIBLE
for (let i = 18; i <= 20; i++) {
    for (let j = 17; j <= 23; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// CORREGIDO: Mesa grande 1 - INACCESSIBLE COMPLETO
for (let i = 24; i <= 27; i++) {
    for (let j = 30; j <= 38; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// CORREGIDO: Mesa grande 2 - INACCESSIBLE COMPLETO
for (let i = 33; i <= 36; i++) {
    for (let j = 31; j <= 38; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Obstáculo superior - INACCESSIBLE
for (let i = 1; i <= 3; i++) {
    for (let j = 21; j <= 23; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Mesas sillón - INACCESSIBLE
for (let i = 4; i <= 5; i++) {
    for (let j = 33; j <= 37; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 7; i <= 7; i++) {
    for (let j = 33; j <= 35; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 6; i <= 8; i++) {
    for (let j = 36; j <= 38; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Obstáculos pequeños - INACCESSIBLE
for (let i = 7; i <= 9; i++) {
    for (let j = 28; j <= 29; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 28; i <= 29; i++) {
    for (let j = 25; j <= 26; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 36; i <= 37; i++) {
    for (let j = 25; j <= 26; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Puntos individuales inaccesibles
edificioGobiernoCollisionMatrix[30][33] = 3;
edificioGobiernoCollisionMatrix[30][35] = 3;
edificioGobiernoCollisionMatrix[30][37] = 3;
edificioGobiernoCollisionMatrix[16][25] = 3;
edificioGobiernoCollisionMatrix[19][25] = 3;
edificioGobiernoCollisionMatrix[21][25] = 3;
edificioGobiernoCollisionMatrix[38][35] = 3;
edificioGobiernoCollisionMatrix[38][37] = 3;
edificioGobiernoCollisionMatrix[38][38] = 3;
edificioGobiernoCollisionMatrix[13][32] = 3;
edificioGobiernoCollisionMatrix[13][37] = 3;
edificioGobiernoCollisionMatrix[10][37] = 3;
edificioGobiernoCollisionMatrix[10][36] = 3;
edificioGobiernoCollisionMatrix[10][34] = 3;

// Columna inaccesible
for (let i = 15; i <= 22; i++) {
    for (let j = 29; j <= 30; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Paredes interiores - INACCESSIBLE
for (let i = 23; i <= 23; i++) {
    for (let j = 24; j <= 27; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 6; i <= 13; i++) {
    for (let j = 17; j <= 17; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 14; i <= 22; i++) {
    for (let j = 27; j <= 27; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 27; i <= 32; i++) {
    for (let j = 27; j <= 27; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 32; i <= 32; i++) {
    for (let j = 24; j <= 27; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 36; i <= 38; i++) {
    for (let j = 27; j <= 27; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 4; i <= 4; i++) {
    for (let j = 26; j <= 28; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 1; i <= 4; i++) {
    for (let j = 27; j <= 27; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 11; i <= 11; i++) {
    for (let j = 30; j <= 34; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 11; i <= 14; i++) {
    for (let j = 34; j <= 34; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 14; i <= 14; i++) {
    for (let j = 31; j <= 38; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}
for (let i = 11; i <= 11; i++) {
    for (let j = 37; j <= 38; j++) {
        edificioGobiernoCollisionMatrix[i][j] = 3;
    }
}

// Punto interactivo para salir al mapa principal
edificioGobiernoCollisionMatrix[2][20] = 0;

// ========== VERIFICACIÓN FINAL ==========

// Asegurar que los pasillos principales se mantengan como PATH
// (sobreescribir cualquier asignación incorrecta anterior)
const pasillosPrincipales = [
    // Fila 5 (horizontal superior)
    {startX: 1, endX: 38, y: 5},
    // Fila 35 (horizontal inferior)
    {startX: 1, endX: 38, y: 35},
    // Columna 5 (vertical izquierdo)
    {x: 5, startY: 1, endY: 38},
    // Columna 35 (vertical derecho)
    {x: 35, startY: 1, endY: 38}
];

pasillosPrincipales.forEach(pasillo => {
    if (pasillo.y !== undefined) {
        // Es un pasillo horizontal
        for (let x = pasillo.startX; x <= pasillo.endX; x++) {
            edificioGobiernoCollisionMatrix[pasillo.y][x] = 2;
        }
    } else {
        // Es un pasillo vertical
        for (let y = pasillo.startY; y <= pasillo.endY; y++) {
            edificioGobiernoCollisionMatrix[y][pasillo.x] = 2;
        }
    }
});

// ==================================
//   Matriz de colisión para ESIME
// ==================================
const esimeCollisionMatrix = Array(40).fill().map(() => Array(40).fill(2)); 

// ========== BORDES DEL MAPA ==========
for (let i = 0; i < 40; i++) {
    esimeCollisionMatrix[0][i] = 1; // Pared superior
    esimeCollisionMatrix[39][i] = 1; // Pared inferior
    esimeCollisionMatrix[i][0] = 1; // Pared izquierda
    esimeCollisionMatrix[i][39] = 1; // Pared derecha
}

// SINCRONIZADO CON Esime.kt - Usando coordenadas exactas de collisionAreas

// Edificio 1 - Rectángulos bloqueados basados en Esime.kt
// Rect(7, 28, 14, 29) - Rectángulo grande desde entrada del Edificio 1
for (let i = 7; i <= 14; i++) {
    for (let j = 28; j <= 29; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(16, 28, 17, 29) - Cuadrado que deja pasillo
for (let i = 16; i <= 17; i++) {
    for (let j = 28; j <= 29; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(7, 31, 14, 32) - Parte inferior
for (let i = 7; i <= 14; i++) {
    for (let j = 31; j <= 32; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 2 - Rectángulos bloqueados basados en Esime.kt
// Rect(7, 22, 14, 23) - Rectángulo grande desde entrada del Edificio 2
for (let i = 7; i <= 14; i++) {
    for (let j = 22; j <= 23; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(16, 22, 17, 23) - Cuadrado que deja pasillo
for (let i = 16; i <= 17; i++) {
    for (let j = 22; j <= 23; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(7, 25, 14, 26) - Parte inferior
for (let i = 7; i <= 14; i++) {
    for (let j = 25; j <= 26; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 3 - Solo bloquear área derecha, dejar entrada libre frontal
// Rect(7, 15, 14, 16) - Área derecha bloqueada del Edificio 3
for (let i = 7; i <= 14; i++) {
    for (let j = 15; j <= 16; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(16, 15, 17, 16) - Cuadrado que deja pasillo
for (let i = 16; i <= 17; i++) {
    for (let j = 15; j <= 16; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(7, 18, 14, 19) - Parte inferior
for (let i = 7; i <= 14; i++) {
    for (let j = 18; j <= 19; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 4 - Rectángulos bloqueados basados en Esime.kt
// Rect(7, 9, 14, 10) - Rectángulo grande desde entrada del Edificio 4
for (let i = 7; i <= 14; i++) {
    for (let j = 9; j <= 10; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(16, 9, 17, 10) - Cuadrado que deja pasillo
for (let i = 16; i <= 17; i++) {
    for (let j = 9; j <= 10; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(7, 12, 14, 13) - Parte inferior
for (let i = 7; i <= 14; i++) {
    for (let j = 12; j <= 13; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 5 - Rectángulos bloqueados basados en Esime.kt
// Rect(7, 3, 14, 4) - Rectángulo grande desde entrada del Edificio 5
for (let i = 7; i <= 14; i++) {
    for (let j = 3; j <= 4; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(16, 3, 17, 4) - Cuadrado que deja pasillo
for (let i = 16; i <= 17; i++) {
    for (let j = 3; j <= 4; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}
// Rect(7, 6, 14, 7) - Parte superior edificio 5
for (let i = 7; i <= 14; i++) {
    for (let j = 6; j <= 7; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// ========== ÁREAS ADICIONALES BLOQUEADAS ==========
// Pastos - Rect(7, 34, 38, 38)
for (let i = 7; i <= 38; i++) {
    for (let j = 34; j <= 38; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Pastos laterales - Rect(32, 29, 38, 38)
for (let i = 32; i <= 38; i++) {
    for (let j = 29; j <= 38; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Área central bloqueada - Rect(24, 6, 29, 18)
for (let i = 24; i <= 29; i++) {
    for (let j = 6; j <= 18; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Zona inaccesible superior - Rect(7, 1, 38, 4)
for (let i = 7; i <= 38; i++) {
    for (let j = 1; j <= 4; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// ========== PUNTOS INTERACTIVOS ==========
// Sincronizado con las definiciones de transición en MapMatrixProvider.kt

// Entrada al Edificio 3 (accesible) - Posición de entrada frontal libre
esimeCollisionMatrix[17][8] = 0; // INTERACTIVE - Entrada Edificio 3

// Punto de transición ESIME a Zacatenco (basado en ESIME_TO_ZACATENCO_POSITION)
esimeCollisionMatrix[2][35] = 0; // INTERACTIVE - Salida a Zacatenco

// Punto de transición Zacatenco a ESIME (basado en ZACATENCO_TO_ESIME_POSITION) 
esimeCollisionMatrix[38][2] = 0; // INTERACTIVE - Entrada desde Zacatenco

// ========== ZONAS CAMINABLES GARANTIZADAS ==========
// Asegurar que el área izquierda de los edificios sea caminable
for (let i = 1; i <= 38; i++) {
    for (let j = 1; j <= 7; j++) {
        if (esimeCollisionMatrix[j][i] !== 0) { // No sobreescribir puntos interactivos - Formato [j][i]
            esimeCollisionMatrix[j][i] = 2; // PATH - Formato [j][i]
        }
    }
}

// Asegurar pasillos horizontales entre edificios
for (let j = 1; j <= 7; j++) {
    // Pasillo entre Edificio 5 y 4
    esimeCollisionMatrix[j][11] = 2; // Formato [j][i]
    // Pasillo entre Edificio 4 y 3
    esimeCollisionMatrix[j][17] = 2; // Formato [j][i]
    // Pasillo entre Edificio 3 y 2
    esimeCollisionMatrix[j][23] = 2; // Formato [j][i]
    // Pasillo entre Edificio 2 y 1
    esimeCollisionMatrix[j][30] = 2; // Formato [j][i]
}

// ========== ASEGURAR CONECTIVIDAD ==========
function asegurarCaminoESIME(startRow, startCol, endRow, endCol) {
    // Camino horizontal - Formato [j][i]
    for (let i = Math.min(startCol, endCol); i <= Math.max(startCol, endCol); i++) {
        if (esimeCollisionMatrix[startRow][i] !== 0) {
            esimeCollisionMatrix[startRow][i] = 2;
        }
    }
    // Camino vertical - Formato [j][i]
    for (let j = Math.min(startRow, endRow); j <= Math.max(startRow, endRow); j++) {
        if (esimeCollisionMatrix[j][endCol] !== 0) {
            esimeCollisionMatrix[j][endCol] = 2;
        }
    }
}

// Conectar puntos importantes - Formato [j][i]
asegurarCaminoESIME(2, 2, 5, 35); // Inicio -> Salida Zacatenco
asegurarCaminoESIME(2, 2, 7, 17); // Inicio -> Entrada Edificio 3
asegurarCaminoESIME(5, 35, 7, 17); // Salida -> Entrada Edificio 3


// =================================================================
// Matriz de colisión para Palapas ISC (40x40)
// =================================================================
const palapasISCCollisionMatrix = Array(40).fill().map(() => Array(40).fill(0));

// 1. Bordes exteriores (Muro = 1)
for (let i = 0; i < 40; i++) {
    palapasISCCollisionMatrix[0][i] = 1;      // Pared superior
    palapasISCCollisionMatrix[39][i] = 1;     // Pared inferior
    palapasISCCollisionMatrix[i][0] = 1;      // Pared izquierda
    palapasISCCollisionMatrix[i][39] = 1;     // Pared derecha
}

// 2. Obstáculos internos (Inaccesible = 1)
// Mesas (6 en total)
for (let i = 8; i <= 11; i++) {
    for (let j = 4; j <= 7; j++) { palapasISCCollisionMatrix[i][j] = 1; }
    for (let j = 14; j <= 17; j++) { palapasISCCollisionMatrix[i][j] = 1; }
    for (let j = 24; j <= 27; j++) { palapasISCCollisionMatrix[i][j] = 1; }
}
for (let i = 21; i <= 24; i++) {
    for (let j = 9; j <= 11; j++) { palapasISCCollisionMatrix[i][j] = 1; }
    for (let j = 18; j <= 21; j++) { palapasISCCollisionMatrix[i][j] = 1; }
    for (let j = 28; j <= 31; j++) { palapasISCCollisionMatrix[i][j] = 1; }
}

// Jardineras (3 en total)
for (let i = 31; i <= 32; i++) {
    for (let j = 2; j <= 15; j++) { palapasISCCollisionMatrix[i][j] = 1; }
    for (let j = 20; j <= 33; j++) { palapasISCCollisionMatrix[i][j] = 1; }
}
for (let i = 6; i <= 25; i++) {
    for (let j = 35; j <= 36; j++) { palapasISCCollisionMatrix[i][j] = 1; }
}

// 3. Punto de salida (Interactivo = 2)
// Coordenadas: (x: 38, y: 38)
palapasISCCollisionMatrix[38][38] = 2;

// =================================================================
// Matriz de colisión para ESIA (40x40)
// =================================================================
const esiaCollisionMatrix = Array(40).fill().map(() => Array(40).fill(1)); // Todo WALL por defecto

// Crear un área rectangular simple y grande para toda la ESIA
for (let i = 3; i < 37; i++) {
    for (let j = 3; j < 37; j++) {
        esiaCollisionMatrix[i][j] = 2; // PATH
    }
}

// Bloquear la zona superior derecha (figura roja grande)
for (let i = 3; i < 12; i++) {
    for (let j = 20; j < 37; j++) {
        const diagonal = (i - 3) + (j - 20);
        if (diagonal > 6) {
            esiaCollisionMatrix[i][j] = 1; // WALL
        }
    }
}

// Bloquear más zona superior derecha (extensión de la figura roja)
for (let i = 3; i < 8; i++) {
    for (let j = 15; j < 37; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear la zona media derecha (figura verde compleja)
for (let i = 15; i < 25; i++) {
    for (let j = 28; j < 37; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Extensión adicional de la zona verde (parte más irregular)
for (let i = 18; i < 22; i++) {
    for (let j = 25; j < 28; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear el área verde específica que encerraste (zona superior derecha)
for (let i = 8; i < 15; i++) {
    for (let j = 25; j < 37; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área adicional (20,12) a (24,12)
for (let i = 12; i < 13; i++) {
    for (let j = 20; j < 25; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área adicional (20,13) a (24,13)
for (let i = 13; i < 14; i++) {
    for (let j = 20; j < 25; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área adicional (20,14) a (24,14)
for (let i = 14; i < 15; i++) {
    for (let j = 20; j < 25; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área adicional (21,15) a (27,15)
for (let i = 15; i < 16; i++) {
    for (let j = 21; j < 28; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área adicional (22,16) a (27,16)
for (let i = 16; i < 17; i++) {
    for (let j = 22; j < 28; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área adicional (23,17) a (27,17)
for (let i = 17; i < 18; i++) {
    for (let j = 23; j < 28; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear puntos específicos adicionales
// Punto (29, 25)
esiaCollisionMatrix[25][29] = 1; // WALL

// Punto (27, 22)
esiaCollisionMatrix[22][27] = 1; // WALL

// Punto (24, 18)
esiaCollisionMatrix[18][24] = 1; // WALL

// Bloquear área (16,8) a (21,8)
for (let i = 8; i < 9; i++) {
    for (let j = 16; j < 22; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área (17,9) a (20,9)
for (let i = 9; i < 10; i++) {
    for (let j = 17; j < 21; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear área (18,10) a (19,10)
for (let i = 10; i < 11; i++) {
    for (let j = 18; j < 20; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear punto (19,11)
esiaCollisionMatrix[11][19] = 1; // WALL

// Bloquear la zona inferior derecha (triángulo inferior)
for (let i = 25; i < 37; i++) {
    for (let j = 30; j < 37; j++) {
        const diagonal = (37 - i) + (j - 30);
        if (diagonal > 8) {
            esiaCollisionMatrix[i][j] = 1; // WALL
        }
    }
}

// Bloquear figura negra superior izquierda
for (let i = 3; i < 10; i++) {
    for (let j = 3; j < 12; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear figura negra superior derecha (zona más específica)
for (let i = 3; i < 7; i++) {
    for (let j = 12; j < 20; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Bloquear el rectángulo superior final
for (let i = 3; i < 8; i++) {
    for (let j = 8; j < 30; j++) {
        esiaCollisionMatrix[i][j] = 1; // WALL
    }
}

// Punto de salida hacia Zacatenco (puerta principal en la parte inferior)
esiaCollisionMatrix[35][25] = 0; // INTERACTIVE

// =================================================================
// Matriz de colisión para Plaza Vista Norte (40x40)
// =================================================================
const plazaVistaNorteCollisionMatrix = Array(40).fill().map(() => Array(40).fill(2)); // Todo PATH por defecto

// Bordes exteriores (WALL = 1)
for (let i = 0; i < 40; i++) {
    plazaVistaNorteCollisionMatrix[0][i] = 1;      // Pared superior
    plazaVistaNorteCollisionMatrix[39][i] = 1;     // Pared inferior
    plazaVistaNorteCollisionMatrix[i][0] = 1;      // Pared izquierda
    plazaVistaNorteCollisionMatrix[i][39] = 1;     // Pared derecha
}

// Jardineras (INACCESSIBLE = 3)
// Jardinera superior
for (let i = 5; i <= 10; i++) {
    for (let j = 5; j <= 35; j++) {
        plazaVistaNorteCollisionMatrix[i][j] = 3;
    }
}
// Jardinera inferior
for (let i = 30; i <= 35; i++) {
    for (let j = 5; j <= 35; j++) {
        plazaVistaNorteCollisionMatrix[i][j] = 3;
    }
}

// Bancas (INACCESSIBLE = 3)
plazaVistaNorteCollisionMatrix[15][10] = 3; plazaVistaNorteCollisionMatrix[15][11] = 3;
plazaVistaNorteCollisionMatrix[15][28] = 3; plazaVistaNorteCollisionMatrix[15][29] = 3;
plazaVistaNorteCollisionMatrix[25][10] = 3; plazaVistaNorteCollisionMatrix[25][11] = 3;
plazaVistaNorteCollisionMatrix[25][28] = 3; plazaVistaNorteCollisionMatrix[25][29] = 3;

// Puntos interactivos (INTERACTIVE = 0)
// Punto de transición para volver a Lindavista
plazaVistaNorteCollisionMatrix[6][1] = 0;



// =================================================================
// Matriz de colisión para Metro Politécnico (40x40)
// =================================================================
const MAP_WIDTH = 40;
const MAP_HEIGHT = 40;

const WALL = 1;
const PATH = 2;
const INTERACTIVE = 0;
const INACCESSIBLE = 3;

const metroPolitecnicoCollisionMatrix = Array(MAP_HEIGHT).fill().map(() => Array(MAP_WIDTH).fill(PATH));

//Bordes exteriores
for (let i = 0; i < MAP_HEIGHT; i++) {
    for (let j = 0; j < MAP_WIDTH; j++) {
        if (i === 0 || i === MAP_HEIGHT - 1 || j === 0 || j === MAP_WIDTH - 1) {
            metroPolitecnicoCollisionMatrix[i][j] = WALL;
        }
    }
}

// Paredes superiores e inferiores
for (let i = 1; i <= 2; i++) {
    for (let j = 0; j < MAP_WIDTH; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
for (let i = 37; i <= 38; i++) {
    for (let j = 0; j < MAP_WIDTH; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}

// Paredes laterales
for (let i = 0; i < MAP_HEIGHT; i++) {
    for (let j = 34; j < MAP_WIDTH; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
for (let i = 0; i < 18; i++) {
    for (let j = 0; j < 13; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
for (let i = 0; i < 18; i++) {
    for (let j = 24; j < MAP_WIDTH; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
for (let i = 22; i < MAP_HEIGHT; i++) {
    for (let j = 0; j < 12; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
for (let i = 31; i < MAP_HEIGHT; i++) {
    for (let j = 19; j < MAP_WIDTH; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}

// Paredes internas
let altura = 30;
for (let j = 20; j < MAP_WIDTH; j++) metroPolitecnicoCollisionMatrix[altura][j] = WALL;
altura = 29;
for (let j = 21; j < MAP_WIDTH; j++) metroPolitecnicoCollisionMatrix[altura][j] = WALL;

for (let i = 22; i < 27; i++) {
    for (let j = 24; j < MAP_WIDTH; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
for (let i = 3; i < 8; i++) {
    for (let j = 20; j < 26; j++) {
        metroPolitecnicoCollisionMatrix[i][j] = WALL;
    }
}
altura = 8;
for (let j = 20; j < MAP_WIDTH; j++) metroPolitecnicoCollisionMatrix[altura][j] = WALL;
altura = 9;
for (let j = 21; j < MAP_WIDTH; j++) metroPolitecnicoCollisionMatrix[altura][j] = WALL;
for (let i = 10; i < 13; i++) {
    for (let j = 22; j < 26; j++) metroPolitecnicoCollisionMatrix[i][j] = WALL;
}

let ancho = 13;
for (let i = 8; i < 14; i++) metroPolitecnicoCollisionMatrix[i][ancho] = WALL;
ancho = 14;
for (let i = 9; i < 13; i++) metroPolitecnicoCollisionMatrix[i][ancho] = WALL;

ancho = 12;
for (let i = 26; i < 31; i++) metroPolitecnicoCollisionMatrix[i][ancho] = WALL;
ancho = 13;
for (let i = 27; i < 30; i++) metroPolitecnicoCollisionMatrix[i][ancho] = WALL;

metroPolitecnicoCollisionMatrix[27][33] = WALL;
metroPolitecnicoCollisionMatrix[28][33] = WALL;
metroPolitecnicoCollisionMatrix[27][32] = WALL;
metroPolitecnicoCollisionMatrix[28][32] = WALL;

// Puntos interactivos
// Taquilla arriba
metroPolitecnicoCollisionMatrix[11][21] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[12][21] = INTERACTIVE;

// Torniquetes izquierda
metroPolitecnicoCollisionMatrix[18][10] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[19][10] = WALL;
metroPolitecnicoCollisionMatrix[20][10] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[21][10] = INTERACTIVE;

// Torniquetes derecha
metroPolitecnicoCollisionMatrix[18][26] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[19][26] = WALL;
metroPolitecnicoCollisionMatrix[20][26] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[21][26] = INTERACTIVE;

// Taquillas
metroPolitecnicoCollisionMatrix[27][31] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[28][31] = INTERACTIVE;

// Máquinas abajo
metroPolitecnicoCollisionMatrix[27][14] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[28][14] = INTERACTIVE;

// Puestos
metroPolitecnicoCollisionMatrix[21][12] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[20][32] = INTERACTIVE;

// Escaleras
metroPolitecnicoCollisionMatrix[18][6] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[21][6] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[21][29] = INTERACTIVE;
metroPolitecnicoCollisionMatrix[18][29] = INTERACTIVE;

// Mural
metroPolitecnicoCollisionMatrix[19][1] = INTERACTIVE;

// Mapa del Metro
metroPolitecnicoCollisionMatrix[24][12] = INTERACTIVE;

// Completar celdas vacías con PATH (Espacio caminable)
for (let i = 0; i < MAP_HEIGHT; i++) {
    for (let j = 0; j < MAP_WIDTH; j++) {
        if (metroPolitecnicoCollisionMatrix[i][j] !== WALL &&
            metroPolitecnicoCollisionMatrix[i][j] !== INTERACTIVE &&
            metroPolitecnicoCollisionMatrix[i][j] !== INACCESSIBLE) {
            metroPolitecnicoCollisionMatrix[i][j] = PATH;
        }
    }
}

/*LAB POSGRADO*/
// Inicializa la matriz con el valor de PATH (2)
const labPosgradoMatrix = Array.from({ length: 40 }, () =>
  Array(40).fill(2)
);

// Bordes exteriores del laboratorio
for (let i = 0; i < 40; i++) {
    for (let j = 0; j < 40; j++) {
        if (i === 0 || i === 40 - 1 || j === 0 || j === 40 - 1) {
            labPosgradoMatrix[i][j] = 1; // WALL
        }
    }
}

// Pantalla del proyector en la pared derecha (inaccesible)
const projectorScreenStart = 40 / 2 - 5;
const projectorScreenEnd = 40 / 2 + 5;
for (let i = projectorScreenStart; i <= projectorScreenEnd; i++) {
    labPosgradoMatrix[i][40 - 2] = 3; // INACCESSIBLE
}

// Mesa del profesor en la parte frontal (derecha, cerca del proyector)
for (let i = projectorScreenStart - 4; i < projectorScreenStart; i++) {
    for (let j = 40 - 15; j < 40 - 8; j++) {
        labPosgradoMatrix[i][j] = 3; // INACCESSIBLE
    }
}

// Filas de computadoras en una cuadrícula
// 4 filas de computadoras
for (let row = 0; row <= 3; row++) {
    const rowY = 8 + (row * 7); // Separación vertical entre filas

    // 5 estaciones de cómputo por fila
    for (let station = 0; station <= 4; station++) {
        const stationX = 3 + (station * 4); // Separación horizontal

        // Cada estación es un bloque de 2x2
        for (let i = rowY; i <= rowY + 1; i++) {
            for (let j = stationX; j <= stationX + 1; j++) {
                if (i < 40 && j < 40) {
                    labPosgradoMatrix[i][j] = 3; // INACCESSIBLE
                }
            }
        }
    }
}

// Puerta de entrada/salida en la parte inferior izquierda
labPosgradoMatrix[40 - 4][4] = 0; // INTERACTIVE
/*LAB POSGRADO*/

// Exportar las matrices
module.exports = {
    cafeteriaCollisionMatrix,
    edificioGobiernoCollisionMatrix,
    labPosgradoMatrix,
    palapasISCCollisionMatrix,
    metroPolitecnicoCollisionMatrix,
    esimeCollisionMatrix,
    esiaCollisionMatrix,
    plazaVistaNorteCollisionMatrix
};

// --- Funciones convertidas desde Kotlin -> JavaScript ---
// Asegúrate de que MAP_WIDTH, MAP_HEIGHT, WALL, PATH, INTERACTIVE, INACCESSIBLE estén definidas antes de usar estas funciones.

function createSalonMatrix() {
    // 1. Iniciar la matriz con todo PATH
    const matrix = Array.from({ length: MAP_HEIGHT }, () => Array(MAP_WIDTH).fill(PATH));

    // 2. Dibujar las paredes exteriores del salón.
    for (let i = 0; i < MAP_HEIGHT; i++) {
        for (let j = 0; j < MAP_WIDTH; j++) {
            if (i === 0 || i === MAP_HEIGHT - 1 || j === 0 || j === MAP_WIDTH - 1) {
                matrix[i][j] = WALL;
            }
        }
    }

    // 3. Colocar el pizarrón y la pantalla en la parte superior.
    for (let j = 12; j < MAP_WIDTH - 12; j++) {
        matrix[6][j] = WALL;
    }

    // 4. Colocar el escritorio del profesor (filas 10 a 12, columnas 25 a 29)
    for (let i = 10; i <= 12; i++) {
        for (let j = 25; j <= 29; j++) {
            matrix[i][j] = WALL;
        }
    }

    // 5. Pupitres de estudiantes (grid)
    const numRows = 5;
    const numCols = 8;
    const rowSpacing = 5; // espacio vertical entre pupitres
    const colSpacing = 4; // espacio horizontal entre pupitres
    const startY = 15;
    const startX = 4;

    for (let row = 0; row < numRows; row++) {
        for (let col = 0; col < numCols; col++) {
            const deskY = startY + row * rowSpacing;
            const deskX = startX + col * colSpacing;
            if (deskY >= 0 && deskY < MAP_HEIGHT && deskX >= 0 && deskX < MAP_WIDTH) {
                matrix[deskY][deskX] = WALL;
            }
        }
    }

    // 6. Punto de interacción para la puerta en la esquina superior izquierda (igual que Kotlin)
    if (6 >= 0 && 0 >= 0 && 6 < MAP_HEIGHT && 0 < MAP_WIDTH) {
        matrix[6][0] = INTERACTIVE;
    }

    return matrix;
}

function createBuilding2Matrix() {
    const matrix = Array.from({ length: MAP_HEIGHT }, () => Array(MAP_WIDTH).fill(PATH));

    // Coordenadas clave
    const topWallY = 14;
    const classroomDepth = 8;
    const corridorWallY = topWallY + classroomDepth;
    const corridorHeight = 9;
    const bottomWallY = corridorWallY + corridorHeight;
    const leftWallX = 1;
    const rightWallX = MAP_WIDTH - 4;

    // 1. Muros exteriores y del pasillo
    for (let j = leftWallX; j <= rightWallX; j++) {
        if (topWallY >= 0 && topWallY < MAP_HEIGHT) matrix[topWallY][j] = WALL;
        if (corridorWallY >= 0 && corridorWallY < MAP_HEIGHT) matrix[corridorWallY][j] = WALL;
        if (bottomWallY >= 0 && bottomWallY < MAP_HEIGHT) matrix[bottomWallY][j] = WALL;
    }
    for (let i = topWallY; i <= bottomWallY; i++) {
        if (i >= 0 && i < MAP_HEIGHT) {
            if (leftWallX >= 0 && leftWallX < MAP_WIDTH) matrix[i][leftWallX] = WALL;
            if (rightWallX >= 0 && rightWallX < MAP_WIDTH) matrix[i][rightWallX] = WALL;
        }
    }

    // 2. Paredes verticales entre salones
    const verticalWallPositions = [6, 11, 15, 19, 23, 28, 33];
    for (const wallX of verticalWallPositions) {
        for (let i = topWallY; i <= corridorWallY; i++) {
            if (i >= 0 && i < MAP_HEIGHT && wallX >= 0 && wallX < MAP_WIDTH) {
                matrix[i][wallX] = WALL;
            }
        }
    }

    // 3. Puertas y puntos interactivos
    if (corridorWallY >= 0 && corridorWallY < MAP_HEIGHT) {
        const doors = [2, 7, 12, 16, 20, 24, 29, 34];
        for (const x of doors) {
            if (x >= 0 && x < MAP_WIDTH) matrix[corridorWallY][x] = INTERACTIVE;
        }

        // Escaleras: abrir hueco (16..18) y punto interactivo arriba (corridorWallY - 1, 17)
        for (let j = 16; j <= 18; j++) {
            if (j >= 0 && j < MAP_WIDTH) matrix[corridorWallY][j] = PATH;
        }
        if (corridorWallY - 1 >= 0 && 17 >= 0 && corridorWallY - 1 < MAP_HEIGHT && 17 < MAP_WIDTH) {
            matrix[corridorWallY - 1][17] = INTERACTIVE;
        }
    }

    // 4. Punto de salida al mapa principal
    if (corridorWallY + 2 >= 0 && corridorWallY + 2 < MAP_HEIGHT && leftWallX >= 0 && leftWallX < MAP_WIDTH) {
        matrix[corridorWallY + 2][leftWallX] = INTERACTIVE;
    }

    console.log("Matriz del Edificio 2 (Final) creada y alineada.");
    return matrix;
}

function createBuilding2Piso1Matrix() {
    const matrix = Array.from({ length: MAP_HEIGHT }, () => Array(MAP_WIDTH).fill(PATH));

    const topWallY = 14;
    const classroomDepth = 8;
    const corridorWallY = topWallY + classroomDepth;
    const corridorHeight = 9;
    const bottomWallY = corridorWallY + corridorHeight;
    const leftWallX = 1;
    const rightWallX = MAP_WIDTH - 4;

    // Muros exteriores y del pasillo
    for (let j = leftWallX; j <= rightWallX; j++) {
        if (topWallY >= 0 && topWallY < MAP_HEIGHT) matrix[topWallY][j] = WALL;
        if (corridorWallY >= 0 && corridorWallY < MAP_HEIGHT) matrix[corridorWallY][j] = WALL;
        if (bottomWallY >= 0 && bottomWallY < MAP_HEIGHT) matrix[bottomWallY][j] = WALL;
    }
    for (let i = topWallY; i <= bottomWallY; i++) {
        if (i >= 0 && i < MAP_HEIGHT) {
            if (leftWallX >= 0 && leftWallX < MAP_WIDTH) matrix[i][leftWallX] = WALL;
            if (rightWallX >= 0 && rightWallX < MAP_WIDTH) matrix[i][rightWallX] = WALL;
        }
    }

    // Paredes verticales entre salones
    const verticalWallPositions = [6, 11, 15, 19, 23, 28, 33];
    for (const wallX of verticalWallPositions) {
        for (let i = topWallY; i <= corridorWallY; i++) {
            if (i >= 0 && i < MAP_HEIGHT && wallX >= 0 && wallX < MAP_WIDTH) {
                matrix[i][wallX] = WALL;
            }
        }
    }

    // Puertas y puntos interactivos (piso 1)
    if (corridorWallY >= 0 && corridorWallY < MAP_HEIGHT) {
        const doors = [2, 7, 12, 16, 20, 24, 29, 34];
        for (const x of doors) {
            if (x >= 0 && x < MAP_WIDTH) matrix[corridorWallY][x] = INTERACTIVE;
        }

        // Escaleras: abrir hueco y marcar puntos para subir/bajar
        for (let j = 16; j <= 18; j++) {
            if (j >= 0 && j < MAP_WIDTH) matrix[corridorWallY][j] = PATH;
        }
        if (corridorWallY - 1 >= 0 && 17 >= 0 && corridorWallY - 1 < MAP_HEIGHT && 17 < MAP_WIDTH) {
            matrix[corridorWallY - 1][17] = INTERACTIVE; // punto para bajar
        }
        if (corridorWallY - 3 >= 0 && 17 >= 0 && corridorWallY - 3 < MAP_HEIGHT && 17 < MAP_WIDTH) {
            matrix[corridorWallY - 3][17] = INTERACTIVE; // punto para subir
        }
    }

    console.log("Matriz del Edificio 2 Piso 1 creada y alineada.");
    return matrix;
}

function createBuilding2Piso2Matrix() {
    const matrix = Array.from({ length: MAP_HEIGHT }, () => Array(MAP_WIDTH).fill(PATH));

    const topWallY = 14;
    const classroomDepth = 8;
    const corridorWallY = topWallY + classroomDepth;
    const corridorHeight = 9;
    const bottomWallY = corridorWallY + corridorHeight;
    const leftWallX = 1;
    const rightWallX = MAP_WIDTH - 4;

    // Muros exteriores y del pasillo
    for (let j = leftWallX; j <= rightWallX; j++) {
        if (topWallY >= 0 && topWallY < MAP_HEIGHT) matrix[topWallY][j] = WALL;
        if (corridorWallY >= 0 && corridorWallY < MAP_HEIGHT) matrix[corridorWallY][j] = WALL;
        if (bottomWallY >= 0 && bottomWallY < MAP_HEIGHT) matrix[bottomWallY][j] = WALL;
    }
    for (let i = topWallY; i <= bottomWallY; i++) {
        if (i >= 0 && i < MAP_HEIGHT) {
            if (leftWallX >= 0 && leftWallX < MAP_WIDTH) matrix[i][leftWallX] = WALL;
            if (rightWallX >= 0 && rightWallX < MAP_WIDTH) matrix[i][rightWallX] = WALL;
        }
    }

    // Paredes verticales entre salones
    const verticalWallPositions = [6, 11, 15, 19, 23, 28, 33];
    for (const wallX of verticalWallPositions) {
        for (let i = topWallY; i <= corridorWallY; i++) {
            if (i >= 0 && i < MAP_HEIGHT && wallX >= 0 && wallX < MAP_WIDTH) {
                matrix[i][wallX] = WALL;
            }
        }
    }

    // Puertas y puntos interactivos (piso 2)
    if (corridorWallY >= 0 && corridorWallY < MAP_HEIGHT) {
        const doors = [2, 7, 12, 16, 20, 24, 29, 34];
        for (const x of doors) {
            if (x >= 0 && x < MAP_WIDTH) matrix[corridorWallY][x] = INTERACTIVE;
        }

        // Escaleras: abrir hueco (16..18) y punto interactivo para bajar
        for (let j = 16; j <= 18; j++) {
            if (j >= 0 && j < MAP_WIDTH) matrix[corridorWallY][j] = PATH;
        }
        if (corridorWallY - 1 >= 0 && 17 >= 0 && corridorWallY - 1 < MAP_HEIGHT && 17 < MAP_WIDTH) {
            matrix[corridorWallY - 1][17] = INTERACTIVE; // punto para bajar
        }
    }

    console.log("Matriz del Edificio 2 Piso 2 creada y alineada.");
    return matrix;
}

function createSalon2010Matrix() {
    const matrix = Array.from({ length: MAP_HEIGHT }, () => Array(MAP_WIDTH).fill(PATH));

    for (let i = 0; i < MAP_HEIGHT; i++) {
        for (let j = 0; j < MAP_WIDTH; j++) {
            // Bordes exteriores
            if (i === 0 || i === MAP_HEIGHT - 1 || j === 0 || j === MAP_WIDTH - 1) {
                matrix[i][j] = WALL;
            }
            // Zonas interactivas (ejemplo: entrada al edificio 2)
            else if (i === 10 && j === 15) {
                matrix[i][j] = INTERACTIVE;
            }
            // Obstáculos (árboles, bancas, etc) - patrón
            else if (i % 7 === 0 && j % 8 === 0) {
                matrix[i][j] = INACCESSIBLE;
            }
            // Caminos especiales
            else if ((i % 5 === 0 || j % 5 === 0) && i > 5 && j > 5) {
                matrix[i][j] = PATH;
            } else {
                // ya es PATH por defecto, no hacer nada
            }
        }
    }

    return matrix;
}

module.exports.createSalonMatrix = createSalonMatrix;
module.exports.createBuilding2Matrix = createBuilding2Matrix;
module.exports.createBuilding2Piso1Matrix = createBuilding2Piso1Matrix;
module.exports.createBuilding2Piso2Matrix = createBuilding2Piso2Matrix;
module.exports.createSalon2010Matrix = createSalon2010Matrix;
