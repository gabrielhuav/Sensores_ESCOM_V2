// zombieController.js
// Este archivo contiene la lógica específica para el minijuego de zombies

// Importar matrices de colisión
const { cafeteriaCollisionMatrix, edificioGobiernoCollisionMatrix } = require('./collisionMatrices');

/**
 * Función para verificar si un movimiento es válido para ZOMBIES
 * Los zombies solo pueden moverse a celdas tipo PATH (2)
 */
function isValidMove(x, y, mapName = 'escom_cafeteria') {
    // Verificar límites del mapa
    if (x < 0 || x >= 40 || y < 0 || y >= 40) {
        return false;
    }

    // Seleccionar la matriz de colisión correcta según el mapa
    const collisionMatrix = mapName === 'edificio_gobierno'
        ? edificioGobiernoCollisionMatrix
        : cafeteriaCollisionMatrix;

    // Verificar la matriz de colisión
    const cellType = collisionMatrix[y][x];

    // SOLO las celdas tipo PATH (2) son válidas para zombies
    return cellType === 2;
}

/**
 * Función para verificar si una posición es válida para el JUGADOR
 * Los jugadores pueden estar en PATH (2) e INTERACTIVE (0), pero NO en INACCESSIBLE (3) o WALL (1)
 */
function isValidPlayerPosition(x, y, mapName = 'escom_cafeteria') {
    // Verificar límites del mapa
    if (x < 0 || x >= 40 || y < 0 || y >= 40) {
        console.log(`❌ Jugador fuera de límites: (${x}, ${y})`);
        return false;
    }

    // Seleccionar la matriz de colisión correcta según el mapa
    const collisionMatrix = mapName === 'edificio_gobierno'
        ? edificioGobiernoCollisionMatrix
        : cafeteriaCollisionMatrix;

    // Verificar la matriz de colisión
    const cellType = collisionMatrix[y][x];

    // DEBUG: Verificar posición del jugador
    console.log(`👤 Jugador en (${x}, ${y}): ${cellType} - ${getCellTypeName(cellType)}`);

    // Los jugadores pueden estar en PATH (2) e INTERACTIVE (0)
    const isValid = cellType === 2 || cellType === 0;

    if (!isValid) {
        console.log(`🚫 POSICIÓN INVÁLIDA PARA JUGADOR: (${x}, ${y}) es ${getCellTypeName(cellType)}`);
    } else {
        console.log(`✅ Posición válida para jugador: (${x}, ${y})`);
    }

    return isValid;
}

/**
 * Función para corregir la posición del jugador si está en un lugar inválido
 */
function correctPlayerPosition(playerData, mapName = 'escom_cafeteria') {
    const currentX = playerData.x;
    const currentY = playerData.y;

    // Si la posición actual es válida, no hacer nada
    if (isValidPlayerPosition(currentX, currentY, mapName)) {
        return playerData;
    }

    console.log(`🔄 Corrigiendo posición inválida del jugador: (${currentX}, ${currentY})`);

    // Buscar la posición válida más cercana
    const directions = [
        { x: 0, y: 0 },   // Posición actual
        { x: 1, y: 0 },   // Derecha
        { x: -1, y: 0 },  // Izquierda
        { x: 0, y: 1 },   // Abajo
        { x: 0, y: -1 },  // Arriba
        { x: 1, y: 1 },   // Diagonal inferior derecha
        { x: -1, y: 1 },  // Diagonal inferior izquierda
        { x: 1, y: -1 },  // Diagonal superior derecha
        { x: -1, y: -1 }  // Diagonal superior izquierda
    ];

    // Probar direcciones cercanas primero
    for (const dir of directions) {
        const newX = currentX + dir.x;
        const newY = currentY + dir.y;

        if (isValidPlayerPosition(newX, newY, mapName)) {
            console.log(`✅ Jugador corregido a: (${newX}, ${newY})`);
            return { x: newX, y: newY };
        }
    }

    // Buscar en un radio más amplio si es necesario
    for (let radius = 2; radius <= 10; radius++) {
        for (let dx = -radius; dx <= radius; dx++) {
            for (let dy = -radius; dy <= radius; dy++) {
                // Saltar las posiciones ya verificadas en el radio más pequeño
                if (Math.abs(dx) < radius && Math.abs(dy) < radius) continue;

                const newX = currentX + dx;
                const newY = currentY + dy;

                if (isValidPlayerPosition(newX, newY, mapName)) {
                    console.log(`✅ Jugador corregido a: (${newX}, ${newY}) (radio ${radius})`);
                    return { x: newX, y: newY };
                }
            }
        }
    }

    // Si no se encuentra ninguna posición válida, usar una posición por defecto
    console.log(`⚠️  No se encontró posición válida, usando posición por defecto`);
    return { x: 5, y: 5 }; // Posición segura por defecto
}

/**
 * Función para validar el movimiento del jugador ANTES de aplicarlo
 */
function validatePlayerMovement(fromX, fromY, toX, toY, mapName = 'escom_cafeteria') {
    // Verificar que la posición destino sea válida
    if (!isValidPlayerPosition(toX, toY, mapName)) {
        console.log(`🚫 Movimiento de jugador BLOQUEADO: (${fromX},${fromY}) -> (${toX},${toY}) es INVÁLIDO`);
        return false;
    }

    console.log(`✅ Movimiento de jugador PERMITIDO: (${fromX},${fromY}) -> (${toX},${toY})`);
    return true;
}

/**
 * Función auxiliar para obtener el nombre del tipo de celda
 */
function getCellTypeName(cellType) {
    switch(cellType) {
        case 0: return 'INTERACTIVE';
        case 1: return 'WALL';
        case 2: return 'PATH';
        case 3: return 'INACCESSIBLE';
        default: return `UNKNOWN (${cellType})`;
    }
}

/**
 * Función para calcular distancia entre dos puntos
 */
function calculateDistance(x1, y1, x2, y2) {
    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
}

/**
 * Función para mover un zombie hacia un jugador objetivo
 */
function moveZombieTowardsPlayer(zombie, playerData, mapName = 'escom_cafeteria') {
    const currentX = zombie.position.x;
    const currentY = zombie.position.y;

    console.log(`\n🧟=== MOVIMIENTO ZOMBIE ${zombie.id} ===`);
    console.log(`📍 Posición actual: (${currentX}, ${currentY})`);
    console.log(`🎯 Objetivo jugador: (${playerData.x}, ${playerData.y})`);

    // Calcular dirección hacia el jugador
    const dx = playerData.x - currentX;
    const dy = playerData.y - currentY;

    console.log(`📐 Dirección: dx=${dx}, dy=${dy}`);

    // Generar movimientos preferentes (hacia el jugador)
    const preferredMoves = [
        { x: currentX + (dx > 0 ? 1 : dx < 0 ? -1 : 0), y: currentY }, // Horizontal
        { x: currentX, y: currentY + (dy > 0 ? 1 : dy < 0 ? -1 : 0) }, // Vertical
    ];

    // Generar movimientos alternativos
    const alternativeMoves = [
        { x: currentX + 1, y: currentY }, // Derecha
        { x: currentX - 1, y: currentY }, // Izquierda
        { x: currentX, y: currentY + 1 }, // Abajo
        { x: currentX, y: currentY - 1 }, // Arriba
    ];

    // Combinar todos los movimientos posibles
    const allMoves = [...preferredMoves, ...alternativeMoves];

    console.log(`🔄 Movimientos posibles: ${JSON.stringify(allMoves)}`);

    // Filtrar solo movimientos válidos (solo PATH)
    const validMoves = allMoves.filter(move =>
        isValidMove(move.x, move.y, mapName)
    );

    console.log(`📊 Resultado: ${validMoves.length}/${allMoves.length} movimientos válidos`);

    // Si no hay movimientos válidos, el zombie se queda quieto
    if (validMoves.length === 0) {
        console.log(`🚫 Zombie ${zombie.id} NO PUEDE MOVERSE - SE QUEDA EN (${currentX}, ${currentY})`);
        return;
    }

    let chosenMove;

    // En dificultad normal/alta, elegir el movimiento que más se acerque al jugador
    if (zombie.difficulty >= 2) {
        // Ordenar movimientos por distancia al jugador
        validMoves.sort((a, b) => {
            const distA = calculateDistance(a.x, a.y, playerData.x, playerData.y);
            const distB = calculateDistance(b.x, b.y, playerData.x, playerData.y);
            return distA - distB;
        });

        chosenMove = validMoves[0];
        console.log(`🎯 Zombie ${zombie.id} (difícil) eligió: (${chosenMove.x}, ${chosenMove.y})`);
    } else {
        // En dificultad baja, movimiento aleatorio
        const randomIndex = Math.floor(Math.random() * validMoves.length);
        chosenMove = validMoves[randomIndex];
        console.log(`🎲 Zombie ${zombie.id} (fácil) eligió: (${chosenMove.x}, ${chosenMove.y})`);
    }

    // Actualizar posición del zombie
    zombie.position = chosenMove;
    console.log(`✅ Zombie ${zombie.id} SE MOVIÓ A: (${chosenMove.x}, ${chosenMove.y})\n`);
}

/**
 * Función para mover un zombie de forma aleatoria
 */
function moveZombieRandomly(zombie, mapName = 'escom_cafeteria') {
    const currentX = zombie.position.x;
    const currentY = zombie.position.y;

    console.log(`\n🎲=== MOVIMIENTO ALEATORIO ZOMBIE ${zombie.id} ===`);
    console.log(`📍 Posición actual: (${currentX}, ${currentY})`);

    const possibleMoves = [
        { x: currentX, y: currentY - 1 }, // Arriba
        { x: currentX + 1, y: currentY }, // Derecha
        { x: currentX, y: currentY + 1 }, // Abajo
        { x: currentX - 1, y: currentY }  // Izquierda
    ];

    console.log(`🔄 Movimientos posibles: ${JSON.stringify(possibleMoves)}`);

    const validMoves = possibleMoves.filter(move =>
        isValidMove(move.x, move.y, mapName)
    );

    console.log(`📊 Resultado: ${validMoves.length}/${possibleMoves.length} movimientos válidos`);

    if (validMoves.length > 0) {
        const randomIndex = Math.floor(Math.random() * validMoves.length);
        zombie.position = validMoves[randomIndex];
        console.log(`✅ Zombie ${zombie.id} SE MOVIÓ ALEATORIAMENTE A: (${zombie.position.x}, ${zombie.position.y})\n`);
    } else {
        console.log(`🚫 Zombie ${zombie.id} NO PUEDE MOVERSE ALEATORIAMENTE\n`);
    }
}

/**
 * Función para verificar si un zombie ha atrapado a un jugador
 */
function isPlayerCaught(zombie, playerData) {
    const distance = calculateDistance(
        zombie.position.x, zombie.position.y,
        playerData.x, playerData.y
    );

    const caught = distance <= 1;

    if (caught) {
        console.log(`🎯 ¡ZOMBIE ${zombie.id} ATRAPÓ AL JUGADOR!`);
        console.log(`   Zombie: (${zombie.position.x}, ${zombie.position.y})`);
        console.log(`   Jugador: (${playerData.x}, ${playerData.y})`);
        console.log(`   Distancia: ${distance}`);
    }

    return caught;
}

/**
 * Función para debuggear las posiciones del jugador y zombies
 */
function debugPlayerZombiePositions(zombies, playerData, mapName) {
    console.log(`\n🔍 === DEBUG POSICIONES JUGADOR/ZOMBIES ===`);

    // Verificar posición del jugador
    const playerValid = isValidPlayerPosition(playerData.x, playerData.y, mapName);
    console.log(`👤 Jugador: (${playerData.x}, ${playerData.y}) - Válido: ${playerValid}`);

    if (!playerValid) {
        console.log(`⚠️  ¡JUGADOR EN POSICIÓN INVÁLIDA!`);
    }

    // Verificar posiciones de zombies
    zombies.forEach(zombie => {
        const zombieValid = isValidMove(zombie.position.x, zombie.position.y, mapName);
        console.log(`🧟 Zombie_${zombie.id}: (${zombie.position.x}, ${zombie.position.y}) - Válido: ${zombieValid}`);

        if (!zombieValid) {
            console.log(`⚠️  ¡ZOMBIE_${zombie.id} EN POSICIÓN INVÁLIDA!`);
        }
    });

    console.log(`=== FIN DEBUG ===\n`);
}

/**
 * Función para inicializar y verificar todas las posiciones al inicio del juego
 */
function initializeAndValidateGame(zombies, playerData, mapName) {
    console.log(`\n🎮 === INICIALIZANDO Y VALIDANDO JUEGO ===`);

    // Corregir posición del jugador si es inválida
    const correctedPlayer = correctPlayerPosition(playerData, mapName);
    if (correctedPlayer.x !== playerData.x || correctedPlayer.y !== playerData.y) {
        console.log(`🔄 Jugador movido de (${playerData.x},${playerData.y}) a (${correctedPlayer.x},${correctedPlayer.y})`);
        playerData.x = correctedPlayer.x;
        playerData.y = correctedPlayer.y;
    }

    // Verificar y corregir posiciones de zombies
    zombies.forEach(zombie => {
        if (!isValidMove(zombie.position.x, zombie.position.y, mapName)) {
            console.log(`🔄 Zombie_${zombie.id} en posición inválida: (${zombie.position.x},${zombie.position.y})`);
            // Buscar posición válida para el zombie
            for (let radius = 1; radius <= 5; radius++) {
                let foundValid = false;
                for (let dx = -radius; dx <= radius; dx++) {
                    for (let dy = -radius; dy <= radius; dy++) {
                        const newX = zombie.position.x + dx;
                        const newY = zombie.position.y + dy;
                        if (isValidMove(newX, newY, mapName)) {
                            zombie.position.x = newX;
                            zombie.position.y = newY;
                            console.log(`✅ Zombie_${zombie.id} movido a: (${newX},${newY})`);
                            foundValid = true;
                            break;
                        }
                    }
                    if (foundValid) break;
                }
                if (foundValid) break;
            }
        }
    });

    console.log(`=== INICIALIZACIÓN COMPLETADA ===\n`);

    // Debug final de todas las posiciones
    debugPlayerZombiePositions(zombies, playerData, mapName);

    return { zombies, playerData };
}

// Exportar funciones
module.exports = {
    // Funciones para zombies
    isValidMove,
    calculateDistance,
    moveZombieTowardsPlayer,
    moveZombieRandomly,
    isPlayerCaught,

    // Funciones para jugador
    isValidPlayerPosition,
    correctPlayerPosition,
    validatePlayerMovement,

    // Funciones de utilidad
    getCellTypeName,
    debugPlayerZombiePositions,
    initializeAndValidateGame
};