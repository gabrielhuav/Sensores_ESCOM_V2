// zombieController.js
// Este archivo contiene la lógica específica para el minijuego de zombies

// Importar matrices de colisión
const { cafeteriaCollisionMatrix } = require('./collisionMatrices');

/**
 * Función para verificar si un movimiento es válido
 */
function isValidMove(x, y) {
    // Verificar límites del mapa
    if (x < 0 || x >= 40 || y < 0 || y >= 40) {
        return false;
    }
    
    // Verificar la matriz de colisión (0 = espacio libre, 1 = obstáculo, 2 = interactivo)
    const cellType = cafeteriaCollisionMatrix[y][x]; // Nota: y primero, luego x
    
    // Permitir movimiento solo en espacios libres (0) o interactivos (2)
    return cellType === 0 || cellType === 2;
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
function moveZombieTowardsPlayer(zombie, playerData) {
    // Calcular dirección hacia el jugador
    const dx = playerData.x - zombie.position.x;
    const dy = playerData.y - zombie.position.y;
    
    // Calcular posibles nuevas posiciones
    const horizontalMove = {
        x: zombie.position.x + (dx > 0 ? 1 : -1),
        y: zombie.position.y
    };
    
    const verticalMove = {
        x: zombie.position.x,
        y: zombie.position.y + (dy > 0 ? 1 : -1)
    };
    
    // Verificar si los movimientos son válidos
    const canMoveHorizontal = isValidMove(horizontalMove.x, horizontalMove.y);
    const canMoveVertical = isValidMove(verticalMove.x, verticalMove.y);
    
    // Decidir movimiento basado en preferencia y validez
    let newPosition;
    
    if (Math.abs(dx) > Math.abs(dy) && canMoveHorizontal) {
        newPosition = horizontalMove;
    } else if (Math.abs(dy) >= Math.abs(dx) && canMoveVertical) {
        newPosition = verticalMove;
    } else if (canMoveHorizontal) {
        newPosition = horizontalMove;
    } else if (canMoveVertical) {
        newPosition = verticalMove;
    } else {
        // No hay movimiento válido
        return;
    }
    
    // En dificultad difícil, intentar movimiento diagonal si ambos son válidos
    if (zombie.difficulty >= 3 && Math.random() > 0.6 && canMoveHorizontal && canMoveVertical) {
        const diagonalMove = {
            x: zombie.position.x + (dx > 0 ? 1 : -1),
            y: zombie.position.y + (dy > 0 ? 1 : -1)
        };
        
        if (isValidMove(diagonalMove.x, diagonalMove.y)) {
            newPosition = diagonalMove;
        }
    }
    
    // Actualizar posición del zombie
    zombie.position = newPosition;
}

/**
 * Función para mover un zombie de forma aleatoria
 */
function moveZombieRandomly(zombie) {
    // Generar movimientos aleatorios y elegir uno válido
    const possibleMoves = [
        { x: zombie.position.x, y: Math.max(0, zombie.position.y - 1) }, // Arriba
        { x: Math.min(39, zombie.position.x + 1), y: zombie.position.y }, // Derecha
        { x: zombie.position.x, y: Math.min(39, zombie.position.y + 1) }, // Abajo
        { x: Math.max(0, zombie.position.x - 1), y: zombie.position.y }  // Izquierda
    ];
    
    // Filtrar solo movimientos válidos
    const validMoves = possibleMoves.filter(move => isValidMove(move.x, move.y));
    
    // Si hay movimientos válidos, elegir uno al azar
    if (validMoves.length > 0) {
        const randomIndex = Math.floor(Math.random() * validMoves.length);
        zombie.position = validMoves[randomIndex];
    }
    // Si no hay movimientos válidos, el zombie se queda quieto
}

/**
 * Función para verificar si un zombie ha atrapado a un jugador
 */
function isPlayerCaught(zombie, playerData) {
    const catchDistance = 2; // Distancia para considerar que ha atrapado a un jugador
    const distance = calculateDistance(
        zombie.position.x, zombie.position.y,
        playerData.x, playerData.y
    );
    
    return distance <= catchDistance;
}

// Exportar funciones
module.exports = {
    isValidMove,
    calculateDistance,
    moveZombieTowardsPlayer,
    moveZombieRandomly,
    isPlayerCaught
};