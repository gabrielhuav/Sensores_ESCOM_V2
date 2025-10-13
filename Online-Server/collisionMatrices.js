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

// Matriz de colisión para ESIME (40x40)
const esimeCollisionMatrix = Array(40).fill().map(() => Array(40).fill(2)); // Todo PATH por defecto

// ========== BORDES DEL MAPA ==========
for (let i = 0; i < 40; i++) {
    esimeCollisionMatrix[0][i] = 1; // Pared superior
    esimeCollisionMatrix[39][i] = 1; // Pared inferior
    esimeCollisionMatrix[i][0] = 1; // Pared izquierda
    esimeCollisionMatrix[i][39] = 1; // Pared derecha
}

// ========== EDIFICIOS BLOQUEADOS (RECTÁNGULOS GRANDES) ==========

// Edificio 1 - Rectángulo grande bloqueado (desde X=8 hacia la derecha)
for (let i = 8; i <= 15; i++) {
    for (let j = 25; j <= 35; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 2 - Rectángulo grande bloqueado (desde X=8 hacia la derecha)
for (let i = 8; i <= 15; i++) {
    for (let j = 19; j <= 29; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 3 - Solo bloquear área derecha, dejar entrada libre frontal
for (let i = 8; i <= 15; i++) {
    for (let j = 12; j <= 22; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 4 - Rectángulo grande bloqueado (desde X=8 hacia la derecha)
for (let i = 8; i <= 15; i++) {
    for (let j = 6; j <= 16; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// Edificio 5 - Rectángulo grande bloqueado (desde X=8 hacia la derecha)
for (let i = 8; i <= 15; i++) {
    for (let j = 0; j <= 10; j++) {
        esimeCollisionMatrix[j][i] = 3; // INACCESSIBLE
    }
}

// ========== PUNTOS INTERACTIVOS ==========

// Entrada al Edificio 3 (accesible)
esimeCollisionMatrix[17][8] = 0; // INTERACTIVE - Entrada Edificio 3

// Punto de salida/entrada a Zacatenco
esimeCollisionMatrix[35][5] = 0; // INTERACTIVE - Salida a Zacatenco

// ========== ZONAS CAMINABLES GARANTIZADAS ==========

// Asegurar que el área izquierda de los edificios sea caminable
for (let i = 1; i <= 7; i++) {
    for (let j = 1; j <= 38; j++) {
        if (esimeCollisionMatrix[j][i] !== 0) { // No sobreescribir puntos interactivos
            esimeCollisionMatrix[j][i] = 2; // PATH
        }
    }
}

// Asegurar pasillo entre edificios
for (let i = 1; i <= 7; i++) {
    // Pasillo entre Edificio 1 y 2
    esimeCollisionMatrix[30][i] = 2;
    // Pasillo entre Edificio 2 y 3
    esimeCollisionMatrix[24][i] = 2;
    // Pasillo entre Edificio 3 y 4
    esimeCollisionMatrix[17][i] = 2;
    // Pasillo entre Edificio 4 y 5
    esimeCollisionMatrix[11][i] = 2;
}

// ========== VERIFICACIÓN DE CONECTIVIDAD ==========

// Garantizar que el jugador pueda llegar a todos los puntos importantes
const puntosImportantes = [
    {x: 5, y: 35}, // Salida a Zacatenco
    {x: 8, y: 17}, // Entrada Edificio 3
    {x: 2, y: 2}   // Posición inicial
];

// Asegurar caminos entre puntos importantes
function asegurarCamino(startX, startY, endX, endY) {
    // Camino horizontal
    for (let x = Math.min(startX, endX); x <= Math.max(startX, endX); x++) {
        if (esimeCollisionMatrix[startY][x] !== 0) {
            esimeCollisionMatrix[startY][x] = 2;
        }
    }
    // Camino vertical
    for (let y = Math.min(startY, endY); y <= Math.max(startY, endY); y++) {
        if (esimeCollisionMatrix[y][endX] !== 0) {
            esimeCollisionMatrix[y][endX] = 2;
        }
    }
}

// Conectar puntos importantes
asegurarCamino(2, 2, 5, 35); // Inicio -> Salida Zacatenco
asegurarCamino(2, 2, 8, 17); // Inicio -> Entrada Edificio 3
asegurarCamino(5, 35, 8, 17); // Salida -> Entrada Edificio 3

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

// Exportar las matrices
module.exports = {
    cafeteriaCollisionMatrix,
    edificioGobiernoCollisionMatrix,
    esimeCollisionMatrix,
    palapasISCCollisionMatrix
};