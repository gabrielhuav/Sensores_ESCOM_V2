// collisionMatrices.js
// Este archivo contiene todas las matrices de colisión para los diferentes mapas

// Matriz de colisión para la cafetería (40x40)
// 0 = espacio libre, 1 = pared/obstáculo, 2 = zona interactiva
const cafeteriaCollisionMatrix = Array(40).fill().map(() => Array(40).fill(0));

// Inicializar los bordes del mapa
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
// Primera fila de mesas
for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 3; col++) {
        // Cada mesa es un rectángulo
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
// Tacos, Burritos, Guacamole y Chile
cafeteriaCollisionMatrix[12][8] = 2;  // Tacos 1
cafeteriaCollisionMatrix[12][32] = 2; // Tacos 2
cafeteriaCollisionMatrix[28][8] = 2;  // Burrito
cafeteriaCollisionMatrix[28][32] = 2; // Chile
cafeteriaCollisionMatrix[20][8] = 2;  // Guacamole

// ENTRADA
for (let i = 37; i <= 38; i++) {
    for (let j = 15; j <= 25; j++) {
        cafeteriaCollisionMatrix[i][j] = 2; // Zona interactiva de entrada/salida
    }
}

// Otras matrices de colisión para otros mapas
// const salon2009CollisionMatrix = ...
// const edificio2CollisionMatrix = ...

// Exportar las matrices
module.exports = {
    cafeteriaCollisionMatrix,
    // También podrás exportar otras matrices aquí
};