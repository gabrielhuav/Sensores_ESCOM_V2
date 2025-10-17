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

// Matriz de colisión para ENCB (40x40)
// Edificio con 6 salas (3 superiores, 3 inferiores) y área central grande
const encbCollisionMatrix = Array(40).fill().map(() => Array(40).fill(2)); // Todo es PATH por defecto

// ============================================
// BORDES EXTERIORES
// ============================================

// Borde superior (fila 0)
for (let j = 0; j < 40; j++) {
    encbCollisionMatrix[0][j] = 1;
}

// Borde inferior (fila 39)
for (let j = 0; j < 40; j++) {
    encbCollisionMatrix[39][j] = 1;
}

// Borde izquierdo (columna 0)
for (let i = 0; i < 40; i++) {
    encbCollisionMatrix[i][0] = 1;
}

// Borde derecho (columna 39)
for (let i = 0; i < 40; i++) {
    encbCollisionMatrix[i][39] = 1;
}

// ============================================
// SALA 1 - SUPERIOR IZQUIERDA (3,3) hasta (10,12)
// ============================================

// Pared superior sala 1
for (let j = 3; j <= 12; j++) {
    encbCollisionMatrix[3][j] = 1;
}

// Pared izquierda sala 1
for (let i = 4; i <= 10; i++) {
    encbCollisionMatrix[i][3] = 1;
}

// Pared derecha sala 1
for (let i = 4; i <= 10; i++) {
    encbCollisionMatrix[i][12] = 1;
}

// Pared inferior sala 1 (con puerta ancha de 3 casillas)
encbCollisionMatrix[10][3] = 1;
encbCollisionMatrix[10][4] = 1;
encbCollisionMatrix[10][5] = 1;
// Puerta: [10][6], [10][7], [10][8] quedan como PATH
encbCollisionMatrix[10][9] = 1;
encbCollisionMatrix[10][10] = 1;
encbCollisionMatrix[10][11] = 1;
encbCollisionMatrix[10][12] = 1;

// Obstáculos internos sala 1 (mesas)
encbCollisionMatrix[5][5] = 3;
encbCollisionMatrix[5][6] = 3;
encbCollisionMatrix[5][9] = 3;
encbCollisionMatrix[5][10] = 3;
encbCollisionMatrix[8][5] = 3;
encbCollisionMatrix[8][6] = 3;
encbCollisionMatrix[8][9] = 3;
encbCollisionMatrix[8][10] = 3;

// ============================================
// SALA 2 - SUPERIOR CENTRO (3,15) hasta (10,24)
// ============================================

// Pared superior sala 2
for (let j = 15; j <= 24; j++) {
    encbCollisionMatrix[3][j] = 1;
}

// Pared izquierda sala 2
for (let i = 4; i <= 10; i++) {
    encbCollisionMatrix[i][15] = 1;
}

// Pared derecha sala 2
for (let i = 4; i <= 10; i++) {
    encbCollisionMatrix[i][24] = 1;
}

// Pared inferior sala 2 (con puerta ancha de 3 casillas)
encbCollisionMatrix[10][15] = 1;
encbCollisionMatrix[10][16] = 1;
encbCollisionMatrix[10][17] = 1;
// Puerta: [10][18], [10][19], [10][20] quedan como PATH
encbCollisionMatrix[10][21] = 1;
encbCollisionMatrix[10][22] = 1;
encbCollisionMatrix[10][23] = 1;
encbCollisionMatrix[10][24] = 1;

// Obstáculos internos sala 2
encbCollisionMatrix[5][17] = 3;
encbCollisionMatrix[5][18] = 3;
encbCollisionMatrix[5][21] = 3;
encbCollisionMatrix[5][22] = 3;
encbCollisionMatrix[8][17] = 3;
encbCollisionMatrix[8][18] = 3;
encbCollisionMatrix[8][21] = 3;
encbCollisionMatrix[8][22] = 3;

// ============================================
// SALA 3 - SUPERIOR DERECHA (3,27) hasta (10,36)
// ============================================

// Pared superior sala 3
for (let j = 27; j <= 36; j++) {
    encbCollisionMatrix[3][j] = 1;
}

// Pared izquierda sala 3
for (let i = 4; i <= 10; i++) {
    encbCollisionMatrix[i][27] = 1;
}

// Pared derecha sala 3
for (let i = 4; i <= 10; i++) {
    encbCollisionMatrix[i][36] = 1;
}

// Pared inferior sala 3 (con puerta ancha de 3 casillas)
encbCollisionMatrix[10][27] = 1;
encbCollisionMatrix[10][28] = 1;
encbCollisionMatrix[10][29] = 1;
// Puerta: [10][30], [10][31], [10][32] quedan como PATH
encbCollisionMatrix[10][33] = 1;
encbCollisionMatrix[10][34] = 1;
encbCollisionMatrix[10][35] = 1;
encbCollisionMatrix[10][36] = 1;

// Obstáculos internos sala 3
encbCollisionMatrix[5][29] = 3;
encbCollisionMatrix[5][30] = 3;
encbCollisionMatrix[5][33] = 3;
encbCollisionMatrix[5][34] = 3;
encbCollisionMatrix[8][29] = 3;
encbCollisionMatrix[8][30] = 3;
encbCollisionMatrix[8][33] = 3;
encbCollisionMatrix[8][34] = 3;

// ============================================
// SALA 4 - INFERIOR IZQUIERDA (29,3) hasta (36,12)
// ============================================

// Pared superior sala 4 (con puerta ancha de 3 casillas)
encbCollisionMatrix[29][3] = 1;
encbCollisionMatrix[29][4] = 1;
encbCollisionMatrix[29][5] = 1;
// Puerta: [29][6], [29][7], [29][8] quedan como PATH
encbCollisionMatrix[29][9] = 1;
encbCollisionMatrix[29][10] = 1;
encbCollisionMatrix[29][11] = 1;
encbCollisionMatrix[29][12] = 1;

// Pared izquierda sala 4
for (let i = 30; i <= 36; i++) {
    encbCollisionMatrix[i][3] = 1;
}

// Pared derecha sala 4
for (let i = 30; i <= 36; i++) {
    encbCollisionMatrix[i][12] = 1;
}

// Pared inferior sala 4
for (let j = 3; j <= 12; j++) {
    encbCollisionMatrix[36][j] = 1;
}

// Obstáculos internos sala 4 (laboratorio - mesas de trabajo)
encbCollisionMatrix[31][5] = 3;
encbCollisionMatrix[31][6] = 3;
encbCollisionMatrix[31][7] = 3;
encbCollisionMatrix[31][9] = 3;
encbCollisionMatrix[31][10] = 3;
encbCollisionMatrix[34][5] = 3;
encbCollisionMatrix[34][6] = 3;
encbCollisionMatrix[34][7] = 3;
encbCollisionMatrix[34][9] = 3;
encbCollisionMatrix[34][10] = 3;

// ============================================
// SALA 5 - INFERIOR CENTRO (29,15) hasta (36,24)
// ============================================

// Pared superior sala 5 (con puerta ancha de 3 casillas)
encbCollisionMatrix[29][15] = 1;
encbCollisionMatrix[29][16] = 1;
encbCollisionMatrix[29][17] = 1;
// Puerta: [29][18], [29][19], [29][20] quedan como PATH
encbCollisionMatrix[29][21] = 1;
encbCollisionMatrix[29][22] = 1;
encbCollisionMatrix[29][23] = 1;
encbCollisionMatrix[29][24] = 1;

// Pared izquierda sala 5
for (let i = 30; i <= 36; i++) {
    encbCollisionMatrix[i][15] = 1;
}

// Pared derecha sala 5
for (let i = 30; i <= 36; i++) {
    encbCollisionMatrix[i][24] = 1;
}

// Pared inferior sala 5
for (let j = 15; j <= 24; j++) {
    encbCollisionMatrix[36][j] = 1;
}

// Obstáculos internos sala 5 (cafetería - mesas)
encbCollisionMatrix[31][17] = 3;
encbCollisionMatrix[31][18] = 3;
encbCollisionMatrix[31][21] = 3;
encbCollisionMatrix[31][22] = 3;
encbCollisionMatrix[34][17] = 3;
encbCollisionMatrix[34][18] = 3;
encbCollisionMatrix[34][21] = 3;
encbCollisionMatrix[34][22] = 3;

// ============================================
// SALA 6 - INFERIOR DERECHA (29,27) hasta (36,36)
// ============================================

// Pared superior sala 6 (con puerta ancha de 3 casillas)
encbCollisionMatrix[29][27] = 1;
encbCollisionMatrix[29][28] = 1;
encbCollisionMatrix[29][29] = 1;
// Puerta: [29][30], [29][31], [29][32] quedan como PATH
encbCollisionMatrix[29][33] = 1;
encbCollisionMatrix[29][34] = 1;
encbCollisionMatrix[29][35] = 1;
encbCollisionMatrix[29][36] = 1;

// Pared izquierda sala 6
for (let i = 30; i <= 36; i++) {
    encbCollisionMatrix[i][27] = 1;
}

// Pared derecha sala 6
for (let i = 30; i <= 36; i++) {
    encbCollisionMatrix[i][36] = 1;
}

// Pared inferior sala 6
for (let j = 27; j <= 36; j++) {
    encbCollisionMatrix[36][j] = 1;
}

// Obstáculos internos sala 6 (biblioteca - estanterías)
encbCollisionMatrix[31][29] = 3;
encbCollisionMatrix[31][30] = 3;
encbCollisionMatrix[31][33] = 3;
encbCollisionMatrix[31][34] = 3;
encbCollisionMatrix[34][29] = 3;
encbCollisionMatrix[34][30] = 3;
encbCollisionMatrix[34][33] = 3;
encbCollisionMatrix[34][34] = 3;

// ============================================
// OBSTÁCULOS DECORATIVOS EN ÁREA CENTRAL
// ============================================

// Jardín/bancas en área central superior
encbCollisionMatrix[15][8] = 3;
encbCollisionMatrix[15][9] = 3;
encbCollisionMatrix[15][19] = 3;
encbCollisionMatrix[15][20] = 3;
encbCollisionMatrix[15][30] = 3;
encbCollisionMatrix[15][31] = 3;

// Jardín/bancas en área central inferior
encbCollisionMatrix[24][8] = 3;
encbCollisionMatrix[24][9] = 3;
encbCollisionMatrix[24][19] = 3;
encbCollisionMatrix[24][20] = 3;
encbCollisionMatrix[24][30] = 3;
encbCollisionMatrix[24][31] = 3;

// Columnas/pilares en el centro
encbCollisionMatrix[17][20] = 3;
encbCollisionMatrix[22][20] = 3;

// ============================================
// PUNTOS INTERACTIVOS
// ============================================

// Marcar puertas como interactivas (centro de cada puerta de 3 casillas)
encbCollisionMatrix[10][7] = 0;   // Puerta sala 1
encbCollisionMatrix[10][19] = 0;  // Puerta sala 2
encbCollisionMatrix[10][31] = 0;  // Puerta sala 3
encbCollisionMatrix[29][7] = 0;   // Puerta sala 4
encbCollisionMatrix[29][19] = 0;  // Puerta sala 5
encbCollisionMatrix[29][31] = 0;  // Puerta sala 6

// Salida principal a Zacatenco (entrada más ancha)
encbCollisionMatrix[20][0] = 0;
encbCollisionMatrix[19][0] = 0;
encbCollisionMatrix[21][0] = 0;


// Exportar las matrices
module.exports = {
    cafeteriaCollisionMatrix,
    edificioGobiernoCollisionMatrix,
    palapasISCCollisionMatrix,
    encbCollisionMatrix
};