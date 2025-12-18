// server.js
// Archivo principal del servidor WebSocket
const express = require("express");
const { WebSocketServer } = require("ws");
const path = require("path");
const { PrismaClient } = require('@prisma/client');

// Importar módulos para el juego de zombies
const {
    isValidMove,
    calculateDistance,
    moveZombieTowardsPlayer,
    moveZombieRandomly,
    isPlayerCaught
} = require('./zombieController');

// Inicializar Prisma Client
const prisma = new PrismaClient();

const app = express();
const PORT = 3000;

const server = app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});

const wss = new WebSocketServer({ server });

const POSITION_THRESHOLD = 1;
const UPDATE_INTERVAL = 50;

const players = {};
const lastUpdateTime = {};

// ============================================
// SISTEMA DE ASIENTOS (PUPITRES)
// ============================================

// Estructura: { mapName: { "x,y": { playerId, playerName, timestamp } } }
const seatedPlayers = {};

// Inicializar asientos para salones
function initializeSeats(mapName) {
    if (!seatedPlayers[mapName]) {
        seatedPlayers[mapName] = {};
    }
}

// Obtener coordenadas de pupitre como string
function getDeskKey(x, y) {
    return `${x},${y}`;
}

// Verificar si un pupitre está ocupado
function isDeskOccupied(mapName, x, y) {
    initializeSeats(mapName);
    const deskKey = getDeskKey(x, y);
    return seatedPlayers[mapName][deskKey] !== undefined;
}

// Sentar a un jugador en un pupitre
function sitPlayer(playerId, playerName, mapName, x, y) {
    initializeSeats(mapName);
    const deskKey = getDeskKey(x, y);

    // Verificar si el pupitre ya está ocupado
    if (isDeskOccupied(mapName, x, y)) {
        const occupant = seatedPlayers[mapName][deskKey];
        if (occupant.playerId === playerId) {
            return { success: false, message: "Ya estás sentado en este pupitre" };
        }
        return { success: false, message: `Pupitre ocupado por ${occupant.playerName}` };
    }

    // Verificar si el jugador ya está sentado en otro lugar del mismo mapa
    for (const [key, seat] of Object.entries(seatedPlayers[mapName])) {
        if (seat.playerId === playerId) {
            return { success: false, message: "Ya estás sentado en otro pupitre. Debes levantarte primero." };
        }
    }

    // Sentar al jugador
    seatedPlayers[mapName][deskKey] = {
        playerId,
        playerName,
        timestamp: Date.now()
    };

    console.log(`✅ ${playerName} se sentó en pupitre (${x}, ${y}) del mapa ${mapName}`);

    return {
        success: true,
        message: `Te sentaste en el pupitre (${x}, ${y})`,
        deskPosition: { x, y }
    };
}

// Levantar a un jugador de su pupitre
function standPlayer(playerId, mapName) {
    initializeSeats(mapName);

    let foundDesk = null;

    // Buscar en qué pupitre está sentado el jugador
    for (const [deskKey, seat] of Object.entries(seatedPlayers[mapName])) {
        if (seat.playerId === playerId) {
            foundDesk = deskKey;
            break;
        }
    }

    if (!foundDesk) {
        return { success: false, message: "No estás sentado en ningún pupitre" };
    }

    const [x, y] = foundDesk.split(',').map(Number);
    const playerName = seatedPlayers[mapName][foundDesk].playerName;

    // Liberar el pupitre
    delete seatedPlayers[mapName][foundDesk];

    console.log(`🚶 ${playerName} se levantó del pupitre (${x}, ${y}) del mapa ${mapName}`);

    return {
        success: true,
        message: `Te levantaste del pupitre (${x}, ${y})`,
        deskPosition: { x, y }
    };
}

// Liberar todos los asientos de un jugador al desconectarse
function releasePlayerSeats(playerId) {
    let releasedSeats = [];

    for (const mapName in seatedPlayers) {
        for (const [deskKey, seat] of Object.entries(seatedPlayers[mapName])) {
            if (seat.playerId === playerId) {
                const [x, y] = deskKey.split(',').map(Number);
                delete seatedPlayers[mapName][deskKey];
                releasedSeats.push({ mapName, x, y });
                console.log(`🔓 Liberado pupitre (${x}, ${y}) del mapa ${mapName} por desconexión de ${seat.playerName}`);
            }
        }
    }

    return releasedSeats;
}

// Obtener todos los asientos ocupados de un mapa
function getOccupiedSeats(mapName) {
    initializeSeats(mapName);
    const seats = [];

    for (const [deskKey, seat] of Object.entries(seatedPlayers[mapName])) {
        const [x, y] = deskKey.split(',').map(Number);
        seats.push({
            x,
            y,
            playerId: seat.playerId,
            playerName: seat.playerName,
            timestamp: seat.timestamp
        });
    }

    return seats;
}

function generateRandomColor() {
    return '#' + Math.floor(Math.random()*16777215).toString(16);
}

function broadcast(data) {
    const message = JSON.stringify(data);
    wss.clients.forEach((client) => {
        if (client.readyState === client.OPEN) {
            client.send(message);
        }
    });
}

function shouldUpdate(playerId) {
    const currentTime = Date.now();
    if (!lastUpdateTime[playerId] || currentTime - lastUpdateTime[playerId] >= UPDATE_INTERVAL) {
        lastUpdateTime[playerId] = currentTime;
        return true;
    }
    return false;
}

function hasPositionChangedSignificantly(oldPos, newPos) {
    if (!oldPos) return true;
    return oldPos.x !== newPos.x || oldPos.y !== newPos.y;
}

function processPosition(data) {
    // Si es una actualización simple
    if (typeof data.x === "number" && typeof data.y === "number") {
        return {
            local: { x: data.x, y: data.y }
        };
    }
    // Si es una actualización con posiciones local y remota
    if (data.local || data.remote) {
        return {
            local: data.local || null,
            remote: data.remote || null
        };
    }
    return null;
}

wss.on("connection", (ws) => {
    console.log("A player connected");
    initializeZombieGame(); // Inicializar el estado del juego zombie

    ws.on("message", (message) => {
        try {
            const data = JSON.parse(message);
            
            // Si el mensaje es una actualización para el mapa global, convierte las coordenadas.

            if (data.type === 'update' && data.map === 'global') {
                if (typeof data.x === 'number') data.x /= 1e6; // Divide la coordenada x
                if (typeof data.y === 'number') data.y /= 1e6; // Divide la coordenada y
        
                // También aplica la lógica si vienen en formato local/remoto
                if (data.local) {
                    data.local.x /= 1e6;
                    data.local.y /= 1e6;
                }
                if (data.remote) {
                    data.remote.x /= 1e6;
                    data.remote.y /= 1e6;
                }
            }
            console.log("Received data:", JSON.stringify(data, null, 2));

            const trimmedId = data.id?.trim();
            if (!trimmedId) return;

        switch (data.type) {
            case "join":
                if (!players[trimmedId]) {
                    players[trimmedId] = {
                        x: 1,
                        y: 1,
                        currentMap: "main",
                        color: generateRandomColor(),
                        type: "local"
                    };
                    console.log(`Player joined: ${trimmedId}`);
                    // Informar al nuevo jugador sobre los jugadores existentes
                    ws.send(JSON.stringify({
                        type: "positions",
                        players: players
                    }));
                }
                break;

            case "update":
                if (shouldUpdate(trimmedId)) {
                    const positions = processPosition(data);
                    const currentMap = data.map || "main";

                    if (positions) {
                        let hasChanges = false;

                        // Actualizar posición local
                        if (positions.local) {
                            const previousPosition = players[trimmedId];
                            if (hasPositionChangedSignificantly(previousPosition, positions.local)) {
                                players[trimmedId] = {
                                    x: positions.local.x,
                                    y: positions.local.y,
                                    currentMap: currentMap,
                                    color: players[trimmedId]?.color || generateRandomColor(),
                                    type: "local"
                                };
                                hasChanges = true;
                            }
                        }

                        // Actualizar posición remota si existe
                        if (positions.remote) {
                            const remoteId = `${trimmedId}_remote`;
                            const previousRemotePosition = players[remoteId];
                            if (hasPositionChangedSignificantly(previousRemotePosition, positions.remote)) {
                                players[remoteId] = {
                                    x: positions.remote.x,
                                    y: positions.remote.y,
                                    currentMap: currentMap,
                                    color: "#FF0000",
                                    type: "remote"
                                };
                                hasChanges = true;
                            }
                        }

                        if (hasChanges) {
                            // Enviar actualización a todos los clientes
                            const updateMessage = {
                                type: "update",
                                id: trimmedId,
                                x: positions.local.x,  // Enviar directamente x e y
                                y: positions.local.y,
                                map: currentMap
                            };
                            broadcast(updateMessage);
                        }
                    }
                }
                break;

            case "leave":
                if (players[trimmedId]) {
                    console.log(`Player left: ${trimmedId}`);
                    delete players[trimmedId];
                    delete players[`${trimmedId}_remote`];
                    delete lastUpdateTime[trimmedId];
                    broadcast({
                        type: "disconnect",
                        id: trimmedId
                    });
                }
                break;

            case "zombie_game_update":
            case "zombie_game_food":
                processZombieGameMessages(data);
                break;

            // ⬇️ AÑADE ESTE NUEVO CASE AQUÍ ⬇️
            case "esimio_game_update":
                processEsimioGameMessages(data);
                break;
        }
                case "leave":
                    if (players[trimmedId]) {
                        console.log(`Player left: ${trimmedId}`);
                        delete players[trimmedId];
                        delete players[`${trimmedId}_remote`];
                        delete lastUpdateTime[trimmedId];
                        broadcast({
                            type: "disconnect",
                            id: trimmedId
                        });
                    }
                    break;

                // Casos de asientos (sentarse/levantarse)
                case "sit":
                    const sitResult = sitPlayer(
                        trimmedId,
                        data.playerName || trimmedId,
                        data.map || "main",
                        data.x,
                        data.y
                    );

                    // Enviar respuesta al jugador que intentó sentarse
                    ws.send(JSON.stringify({
                        type: "sit_response",
                        success: sitResult.success,
                        message: sitResult.message,
                        deskPosition: sitResult.deskPosition
                    }));

                    // Si fue exitoso, notificar a todos los jugadores
                    if (sitResult.success) {
                        broadcast({
                            type: "player_seated",
                            playerId: trimmedId,
                            playerName: data.playerName || trimmedId,
                            map: data.map || "main",
                            x: data.x,
                            y: data.y
                        });
                    }
                    break;

                case "stand":
                    const standResult = standPlayer(trimmedId, data.map || "main");

                    // Enviar respuesta al jugador que intentó levantarse
                    ws.send(JSON.stringify({
                        type: "stand_response",
                        success: standResult.success,
                        message: standResult.message,
                        deskPosition: standResult.deskPosition
                    }));

                    // Si fue exitoso, notificar a todos los jugadores
                    if (standResult.success) {
                        broadcast({
                            type: "player_stood",
                            playerId: trimmedId,
                            map: data.map || "main",
                            x: standResult.deskPosition.x,
                            y: standResult.deskPosition.y
                        });
                    }
                    break;

                case "get_occupied_seats":
                    const occupiedSeats = getOccupiedSeats(data.map || "main");
                    ws.send(JSON.stringify({
                        type: "occupied_seats",
                        map: data.map || "main",
                        seats: occupiedSeats
                    }));
                    break;

                case "zombie_game_update":
                case "zombie_game_food":
                    processZombieGameMessages(data);
                    break;
            }
        } catch (error) {
            console.error("Error processing message:", error);
            console.error(error.stack);
        }
    });

    ws.on("close", () => {
        console.log("A player disconnected");
        // Buscar y eliminar el jugador desconectado
        Object.keys(players).forEach(playerId => {
            if (players[playerId].ws === ws) {
                // Liberar asientos del jugador
                const releasedSeats = releasePlayerSeats(playerId);

                // Notificar a todos sobre los asientos liberados
                releasedSeats.forEach(seat => {
                    broadcast({
                        type: "seat_released",
                        playerId: playerId,
                        map: seat.mapName,
                        x: seat.x,
                        y: seat.y
                    });
                });

                delete players[playerId];
                delete players[`${playerId}_remote`];
                delete lastUpdateTime[playerId];
                broadcast({
                    type: "disconnect",
                    id: playerId
                });
            }
        });
    });
});

app.get("/", (req, res) => {
    res.json({
        message: "WebSocket server is running.",
        connectedPlayers: Object.keys(players).length,
        players: players
    });
});

// Limpieza de jugadores inactivos
setInterval(() => {
    const currentTime = Date.now();
    Object.keys(lastUpdateTime).forEach(playerId => {
        if (currentTime - lastUpdateTime[playerId] > 30000) { // 30 segundos de inactividad
            console.log(`Removing inactive player: ${playerId}`);
            delete players[playerId];
            delete players[`${playerId}_remote`];
            delete lastUpdateTime[playerId];
            broadcast({
                type: "disconnect",
                id: playerId
            });
        }
    });
}, 10000);

// Manejo de errores no capturados
process.on('uncaughtException', (error) => {
    console.error('Uncaught Exception:', error);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

// Estado del zombie
const zombieGame = {
    zombies: [],
    isActive: false,
    difficulty: 1,
    currentMap: "escom_cafeteria",
    lastUpdateTimes: {},  // Para controlar la frecuencia de actualización
    updateIntervals: {
        1: 1200,  // Fácil: 1.2 segundos
        2: 800,   // Medio: 0.8 segundos
        3: 500    // Difícil: 0.5 segundos
    },
    zombieCount: {
        1: 2,  // Fácil: 2 zombies
        2: 4,  // Medio: 4 zombies
        3: 6   // Difícil: 6 zombies
    }
};

function initializeZombieGame() {
    zombieGame.zombies = [];
    zombieGame.isActive = false;
    zombieGame.difficulty = 1;
    zombieGame.currentMap = "escom_cafeteria";
    zombieGame.lastUpdateTimes = {};
    console.log("Estado del juego zombie inicializado:", zombieGame);
}

// Intervalo de actualización del zombie
let zombieUpdateInterval = null;

// Función para iniciar el minijuego del zombie
function startZombieGame(difficulty = 1, mapName = "escom_cafeteria") {
    zombieGame.isActive = true;
    zombieGame.difficulty = difficulty;
    zombieGame.currentMap = mapName;

    // Crear zombies según la dificultad
    const zombieCount = zombieGame.zombieCount[difficulty] || 2;

    // Limpiar zombies existentes
    zombieGame.zombies = [];

    // Función auxiliar para encontrar una posición válida
    const findValidPosition = (attempt = 0) => {
        if (attempt > 100) {
            // Después de 100 intentos, usar posición por defecto
            console.warn('No se pudo encontrar posición válida, usando por defecto');
            return { x: 20, y: 15 };
        }

        // Generar posición aleatoria en el área del mapa
        const x = Math.floor(Math.random() * 35) + 2; // Entre 2 y 37
        const y = Math.floor(Math.random() * 35) + 2; // Entre 2 y 37

        // Verificar si es válida
        if (isValidMove(x, y, mapName)) {
            return { x, y };
        }

        // Si no es válida, intentar de nuevo
        return findValidPosition(attempt + 1);
    };

    // Crear nuevos zombies con posiciones validadas
    for (let i = 0; i < zombieCount; i++) {
        const position = findValidPosition();

        zombieGame.zombies.push({
            id: `zombie_${i}`,
            position: { x: position.x, y: position.y },
            target: null,
            difficulty: difficulty // Añadir la dificultad al objeto zombie
        });

        console.log(`✅ Zombie ${i} creado en posición VÁLIDA: (${position.x}, ${position.y})`);

        // Verificar que la posición es realmente válida
        const cellType = isValidMove(position.x, position.y, mapName) ? 'VÁLIDA' : 'INVÁLIDA';
        console.log(`   Verificación: Posición es ${cellType}`);
    }

    // Notificar a todos los clientes que el juego ha iniciado
    broadcast({
        type: "zombie_game_command",
        command: "start",
        difficulty: difficulty,
        map: mapName
    });

    // Enviar posición inicial de los zombies a todos los clientes
    zombieGame.zombies.forEach(zombie => {
        broadcast({
            type: "zombie_position",
            id: zombie.id,
            x: zombie.position.x,
            y: zombie.position.y,
            map: zombieGame.currentMap
        });
    });

    // Iniciar la actualización periódica
    startZombieUpdates();

    console.log(`Minijuego zombie iniciado con dificultad ${difficulty} en mapa ${mapName}`);
}

// Función para detener el minijuego del zombie
function stopZombieGame() {
    zombieGame.isActive = false;

    // Detener todas las actualizaciones
    clearInterval(zombieUpdateInterval);
    zombieUpdateInterval = null;

    // Notificar a todos los clientes
    broadcast({
        type: "zombie_game_command",
        command: "stop"
    });

    console.log("Minijuego zombie detenido");
}


// Función para iniciar las actualizaciones periódicas del zombie
function startZombieUpdates() {
    // Detener intervalo existente si hay
    if (zombieUpdateInterval) {
        clearInterval(zombieUpdateInterval);
    }

    // El intervalo de actualización depende de la dificultad
    const updateInterval = zombieGame.updateIntervals[zombieGame.difficulty] || 1000;

    // Crear nuevo intervalo
    zombieUpdateInterval = setInterval(() => {
        if (zombieGame.isActive) {
            updateZombiePositions();
        } else {
            clearInterval(zombieUpdateInterval);
            zombieUpdateInterval = null;
        }
    }, updateInterval);
}

// Función para actualizar la posición del zombie
function updateZombiePositions() {
    if (!zombieGame.isActive) return;

    // Buscar jugadores en el mapa actual del juego zombie
    const playersInMap = Object.entries(players).filter(([id, data]) => {
        return data.currentMap === zombieGame.currentMap;
    });

    if (playersInMap.length === 0) {
        // Si no hay jugadores, los zombies se mueven aleatoriamente
        zombieGame.zombies.forEach(zombie => {
            moveZombieRandomly(zombie, zombieGame.currentMap);

            // Enviar actualización
            broadcast({
                type: "zombie_position",
                id: zombie.id,
                x: zombie.position.x,
                y: zombie.position.y,
                map: zombieGame.currentMap
            });
        });
        return;
    }

    // Actualizar cada zombie
    zombieGame.zombies.forEach(zombie => {
        // Encontrar el jugador más cercano
        let nearestPlayer = null;
        let shortestDistance = Infinity;

        playersInMap.forEach(([playerId, playerData]) => {
            const distance = calculateDistance(
                zombie.position.x, zombie.position.y,
                playerData.x, playerData.y
            );

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestPlayer = { id: playerId, data: playerData };
            }
        });

        if (nearestPlayer) {
            // Asignar objetivo
            zombie.target = nearestPlayer.id;

            // Mover hacia el jugador, pasando el nombre del mapa
            moveZombieTowardsPlayer(zombie, nearestPlayer.data, zombieGame.currentMap);

            // Verificar colisión
            if (isPlayerCaught(zombie, nearestPlayer.data)) {
                broadcast({
                    type: "zombie_game_command",
                    command: "caught",
                    player: nearestPlayer.id
                });

                console.log(`Zombie ${zombie.id} atrapó al jugador ${nearestPlayer.id}`);
            }
        } else {
            // Si por alguna razón no encontramos jugador, mover aleatoriamente
            moveZombieRandomly(zombie, zombieGame.currentMap);
        }

        // Enviar actualización
        broadcast({
            type: "zombie_position",
            id: zombie.id,
            x: zombie.position.x,
            y: zombie.position.y,
            map: zombieGame.currentMap
        });
    });
}

const caughtPlayers = new Set();

// Verificar si el zombie ha atrapado a algún jugador
function checkZombieCollisions() {
    const catchDistance = 2; // Distancia para considerar que ha atrapado a un jugador

    Object.entries(players).forEach(([playerId, playerData]) => {
        if (playerData.currentMap === zombieGame.currentMap && !caughtPlayers.has(playerId)) {
            // Recorre todos los zombies y verifica colisiones con cada uno
            zombieGame.zombies.forEach(zombie => {
                const distanceX = Math.abs(zombie.position.x - playerData.x);
                const distanceY = Math.abs(zombie.position.y - playerData.y);

                if (distanceX <= catchDistance && distanceY <= catchDistance) {
                    // Añadir a la lista de atrapados para evitar mensajes duplicados
                    caughtPlayers.add(playerId);

                    // Enviar mensaje al jugador que ha sido atrapado
                    broadcast({
                        type: "zombie_game_command",
                        command: "caught",
                        player: playerId
                    });

                    console.log(`Zombie atrapó al jugador ${playerId} en posición (${playerData.x}, ${playerData.y})`);

                    // Limpiar después de un tiempo para permitir que el jugador sea atrapado de nuevo
                    setTimeout(() => {
                        caughtPlayers.delete(playerId);
                    }, 5000);
                }
            });
        }
    });
}

// Procesar mensajes relacionados con el minijuego del zombie
function processZombieGameMessages(message) {
    if (message.type === "zombie_game_update") {
        const action = message.action;

        switch (action) {
            case "start":
                if (!zombieGame.isActive) {
                    const difficulty = message.difficulty || 1; // Usar dificultad recibida o por defecto
                    const mapName = message.map || "escom_cafeteria"; // Usar mapa recibido o por defecto
                    startZombieGame(difficulty, mapName);
                }
                break;

            case "stop":
                if (zombieGame.isActive) {
                    stopZombieGame();
                }
                break;

            case "complete":
                // Un jugador ha completado el minijuego
                if (zombieGame.isActive) {
                    // Notificar a todos los clientes
                    broadcast({
                        type: "zombie_game_update",
                        action: "player_result",
                        player: message.player,
                        survived: message.survived,
                        time: message.time,
                        score: message.score
                    });

                    console.log(`Jugador ${message.player} completó el minijuego: ${message.survived ? 'Sobrevivió' : 'Atrapado'}, Puntuación: ${message.score}`);
                }
                break;
        }
    } else if (message.type === "zombie_game_food") {
        // Un jugador ha recogido comida - ralentizar a todos los zombies
        broadcast({
            type: "zombie_game_food",
            player: message.player,
            score: message.score,
            x: message.x,
            y: message.y
        });

        // Aumentar temporalmente el intervalo de actualización (más lento)
        const oldInterval = zombieGame.updateIntervals[zombieGame.difficulty];
        const newInterval = oldInterval + 300; // 300ms más lento

        // Detener intervalo actual
        clearInterval(zombieUpdateInterval);

        // Crear nuevo intervalo más lento
        zombieUpdateInterval = setInterval(() => {
            if (zombieGame.isActive) {
                updateZombiePositions();
            } else {
                clearInterval(zombieUpdateInterval);
                zombieUpdateInterval = null;
            }
        }, newInterval);

        // Notificar ralentización
        broadcast({
            type: "zombie_game_command",
            command: "zombie_slowed",
            player: message.player
        });

        // Restaurar velocidad después de 3 segundos
        setTimeout(() => {
            // Solo restaurar si el juego sigue activo
            if (zombieGame.isActive) {
                // Detener intervalo actual
                clearInterval(zombieUpdateInterval);

                // Crear nuevo intervalo con velocidad normal
                zombieUpdateInterval = setInterval(() => {
                    if (zombieGame.isActive) {
                        updateZombiePositions();
                    } else {
                        clearInterval(zombieUpdateInterval);
                        zombieUpdateInterval = null;
                    }
                }, oldInterval);

                // Notificar a los clientes
                broadcast({
                    type: "zombie_game_command",
                    command: "zombie_speed_normal"
                });
            }
        }, 3000);
    }
}

// Middleware para parsear JSON
app.use(express.json());

// ============================================
// ATTENDANCE ROUTES
// ============================================

// Helper function to get current time in Mexico City timezone (UTC-6)
function getMexicoDateTime() {
    // Create a date in UTC
    const now = new Date();
    // Convert to Mexico City timezone (UTC-6)
    const mexicoTime = new Date(now.toLocaleString('en-US', { timeZone: 'America/Mexico_City' }));
    return mexicoTime;
}

// Helper function to get start and end of day in Mexico City timezone
function getMexicoDayBounds() {
    const mexicoNow = getMexicoDateTime();

    // Start of day in Mexico (00:00:00)
    const startOfDay = new Date(mexicoNow);
    startOfDay.setHours(0, 0, 0, 0);

    // End of day in Mexico (23:59:59)
    const endOfDay = new Date(mexicoNow);
    endOfDay.setHours(23, 59, 59, 999);

    return { startOfDay, endOfDay };
}

// Helper function to check if attendance already exists for today
async function hasAttendedToday(phoneID) {
    const { startOfDay, endOfDay } = getMexicoDayBounds();

    const existingAttendance = await prisma.attendance.findFirst({
        where: {
            phoneID: phoneID,
            attendanceTime: {
                gte: startOfDay,
                lte: endOfDay
            }
        }
    });

    return existingAttendance !== null;
}

// POST /attendance - Register attendance
app.post("/attendance", async (req, res) => {
    try {
        const { phoneID, fullName, group } = req.body;

        // Validate required fields
        if (!phoneID || !fullName || !group) {
            return res.status(400).json({
                success: false,
                error: "Missing required fields: phoneID, fullName, and group are required"
            });
        }

        // Check if user already attended today
        const alreadyAttended = await hasAttendedToday(phoneID);

        if (alreadyAttended) {
            return res.status(409).json({
                success: false,
                error: "Attendance already registered for this phoneID today"
            });
        }

        // Get current time in Mexico City timezone
        const mexicoTime = getMexicoDateTime();

        // Create attendance record with Mexico time
        const attendance = await prisma.attendance.create({
            data: {
                phoneID,
                fullName,
                group,
                attendanceTime: mexicoTime
            }
        });

        res.status(201).json({
            success: true,
            message: "Attendance registered successfully",
            data: attendance
        });

    } catch (error) {
        console.error("Error registering attendance:", error);
        res.status(500).json({
            success: false,
            error: "Internal server error while registering attendance"
        });
    }
});

// GET /attendance/:date/:group - Get attendance list for a specific day and group
// Date format: YYYY-MM-DD
app.get("/attendance/:date/:group", async (req, res) => {
    try {
        const { date, group } = req.params;

        // Validate date format
        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRegex.test(date)) {
            return res.status(400).json({
                success: false,
                error: "Invalid date format. Use YYYY-MM-DD"
            });
        }

        // Parse date in Mexico City timezone
        // Add 'T00:00:00' to ensure proper parsing
        const targetDate = new Date(date + 'T00:00:00');
        if (isNaN(targetDate.getTime())) {
            return res.status(400).json({
                success: false,
                error: "Invalid date"
            });
        }

        // Set start and end of day in Mexico timezone
        const startOfDay = new Date(targetDate);
        startOfDay.setHours(0, 0, 0, 0);

        const endOfDay = new Date(targetDate);
        endOfDay.setHours(23, 59, 59, 999);

        // Query attendance records
        const attendanceRecords = await prisma.attendance.findMany({
            where: {
                group: group,
                attendanceTime: {
                    gte: startOfDay,
                    lte: endOfDay
                }
            },
            orderBy: {
                attendanceTime: 'asc'
            },
            select: {
                phoneID: true,
                attendanceTime: true,
                fullName: true,
                group: true
            }
        });

        // Format dates to Mexico timezone for response
        const formattedRecords = attendanceRecords.map(record => ({
            ...record,
            attendanceTime: new Date(record.attendanceTime).toLocaleString('es-MX', {
                timeZone: 'America/Mexico_City',
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            })
        }));

        res.json({
            success: true,
            date: date,
            group: group,
            count: formattedRecords.length,
            attendees: formattedRecords
        });

    } catch (error) {
        console.error("Error fetching attendance:", error);
        res.status(500).json({
            success: false,
            error: "Internal server error while fetching attendance"
        });
    }
});

// ============================================
// END ATTENDANCE ROUTES
// ============================================

// ============================================
// SEATS ROUTES
// ============================================

// GET /seats/:map - Get all occupied seats in a map
app.get("/seats/:map", (req, res) => {
    try {
        const { map } = req.params;
        const occupiedSeats = getOccupiedSeats(map);

        res.json({
            success: true,
            map: map,
            count: occupiedSeats.length,
            seats: occupiedSeats
        });
    } catch (error) {
        console.error("Error fetching seats:", error);
        res.status(500).json({
            success: false,
            error: "Internal server error while fetching seats"
        });
    }
});

// POST /seats/sit - Sit in a desk
app.post("/seats/sit", (req, res) => {
    try {
        const { playerId, playerName, map, x, y } = req.body;

        if (!playerId || !map || x === undefined || y === undefined) {
            return res.status(400).json({
                success: false,
                error: "Missing required fields: playerId, map, x, y"
            });
        }

        const result = sitPlayer(playerId, playerName || playerId, map, x, y);

        if (result.success) {
            // Notificar a todos los clientes WebSocket
            broadcast({
                type: "player_seated",
                playerId: playerId,
                playerName: playerName || playerId,
                map: map,
                x: x,
                y: y
            });

            res.json(result);
        } else {
            res.status(409).json(result);
        }
    } catch (error) {
        console.error("Error sitting player:", error);
        res.status(500).json({
            success: false,
            error: "Internal server error while sitting"
        });
    }
});

// POST /seats/stand - Stand up from desk
app.post("/seats/stand", (req, res) => {
    try {
        const { playerId, map } = req.body;

        if (!playerId || !map) {
            return res.status(400).json({
                success: false,
                error: "Missing required fields: playerId, map"
            });
        }

        const result = standPlayer(playerId, map);

        if (result.success) {
            // Notificar a todos los clientes WebSocket
            broadcast({
                type: "player_stood",
                playerId: playerId,
                map: map,
                x: result.deskPosition.x,
                y: result.deskPosition.y
            });

            res.json(result);
        } else {
            res.status(404).json(result);
        }
    } catch (error) {
        console.error("Error standing player:", error);
        res.status(500).json({
            success: false,
            error: "Internal server error while standing"
        });
    }
});

// DELETE /seats/:map/:playerId - Force release all seats of a player in a map
app.delete("/seats/:map/:playerId", (req, res) => {
    try {
        const { map, playerId } = req.params;

        const result = standPlayer(playerId, map);

        if (result.success) {
            broadcast({
                type: "player_stood",
                playerId: playerId,
                map: map,
                x: result.deskPosition.x,
                y: result.deskPosition.y
            });
        }

        res.json({
            success: true,
            message: "Player seats released",
            result: result
        });
    } catch (error) {
        console.error("Error releasing seats:", error);
        res.status(500).json({
            success: false,
            error: "Internal server error while releasing seats"
        });
    }
});

// ============================================
// END SEATS ROUTES
// ============================================

// Rutas administrativas para controlar el minijuego
app.post("/admin/zombie/start", (req, res) => {
    const difficulty = req.body.difficulty || 1;
    const mapName = req.body.map || "escom_cafeteria";
    startZombieGame(difficulty, mapName);
    res.json({ message: "Minijuego zombie iniciado", state: zombieGame });
});

app.post("/admin/zombie/stop", (req, res) => {
    stopZombieGame();
    res.json({ message: "Minijuego zombie detenido", state: zombieGame });
});

app.get("/admin/zombie/state", (req, res) => {
    res.json(zombieGame);
});

app.get("/admin/zombie/start-test", (req, res) => {
    startZombieGame(1);
    res.json({ message: "Minijuego zombie iniciado para pruebas", state: zombieGame });
});

app.get("/admin/zombie/list", (req, res) => {
    res.json({
        isActive: zombieGame.isActive,
        difficulty: zombieGame.difficulty,
        currentMap: zombieGame.currentMap,
        zombies: zombieGame.zombies,
        zombieCount: zombieGame.zombies.length
    });
});


// Estado del juego de esimios
const esimioGame = {
    esimios: [],
    isActive: false,
    difficulty: 1,
    currentMap: "esime",
    lastUpdateTimes: {},
    updateIntervals: {
        1: 1500,  // Fácil: 1.5 segundos
        2: 1000,  // Medio: 1 segundo
        3: 600    // Difícil: 0.6 segundos
    },
    esimioCount: {
        1: 3,  // Fácil: 3 esimios
        2: 5,  // Medio: 5 esimios
        3: 8   // Difícil: 8 esimios
    }
};

let esimioUpdateInterval = null;

// Función para iniciar el juego de esimios
function startEsimioGame(difficulty = 1, mapName = "esime") {
    esimioGame.isActive = true;
    esimioGame.difficulty = difficulty;
    esimioGame.currentMap = mapName;

    const esimioCount = esimioGame.esimioCount[difficulty] || 3;
    esimioGame.esimios = [];

    // Posiciones de spawn para esimios
    const spawnPositions = [
        { x: 20, y: 10 },
        { x: 20, y: 20 },
        { x: 20, y: 30 },
        { x: 25, y: 15 },
        { x: 25, y: 25 },
        { x: 19, y: 12 },
        { x: 19, y: 22 },
        { x: 19, y: 32 }
    ];

    const findValidPosition = (attempt = 0) => {
        if (attempt > 100) {
            return spawnPositions[0];
        }
        const x = Math.floor(Math.random() * 20) + 18;
        const y = Math.floor(Math.random() * 30) + 5;

        if (isValidEsimioMove(x, y)) {
            return { x, y };
        }
        return findValidPosition(attempt + 1);
    };

    for (let i = 0; i < esimioCount; i++) {
        const position = findValidPosition();
        esimioGame.esimios.push({
            id: `esimio_${i}`,
            position: position,
            target: null
        });

        broadcast({
            type: "esimio_position",
            id: `esimio_${i}`,
            x: position.x,
            y: position.y,
            map: esimioGame.currentMap
        });
    }

    broadcast({
        type: "esimio_game_command",
        command: "start",
        difficulty: difficulty,
        map: mapName
    });

    startEsimioUpdates();
    console.log(`Juego de esimios iniciado con dificultad ${difficulty}`);
}

function stopEsimioGame() {
    esimioGame.isActive = false;
    clearInterval(esimioUpdateInterval);
    esimioUpdateInterval = null;

    broadcast({
        type: "esimio_game_command",
        command: "stop"
    });

    console.log("Juego de esimios detenido");
}

function startEsimioUpdates() {
    if (esimioUpdateInterval) {
        clearInterval(esimioUpdateInterval);
    }

    const updateInterval = esimioGame.updateIntervals[esimioGame.difficulty] || 1500;

    esimioUpdateInterval = setInterval(() => {
        if (esimioGame.isActive) {
            updateEsimioPositions();
        } else {
            clearInterval(esimioUpdateInterval);
            esimioUpdateInterval = null;
        }
    }, updateInterval);
}

function updateEsimioPositions() {
    if (!esimioGame.isActive) return;

    const playersInMap = Object.entries(players).filter(([id, data]) => {
        return data.currentMap === esimioGame.currentMap || data.currentMap === "esime";
    });

    if (playersInMap.length === 0) {
        esimioGame.esimios.forEach(esimio => {
            moveEsimioRandomly(esimio);
            broadcast({
                type: "esimio_position",
                id: esimio.id,
                x: esimio.position.x,
                y: esimio.position.y,
                map: esimioGame.currentMap
            });
        });
        return;
    }

    esimioGame.esimios.forEach(esimio => {
        let nearestPlayer = null;
        let shortestDistance = Infinity;

        playersInMap.forEach(([playerId, playerData]) => {
            const distance = calculateDistance(
                esimio.position.x, esimio.position.y,
                playerData.x, playerData.y
            );

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestPlayer = { id: playerId, data: playerData };
            }
        });

        if (nearestPlayer) {
            esimio.target = nearestPlayer.id;
            moveEsimioTowardsPlayer(esimio, nearestPlayer.data);

            if (isEsimioCaught(esimio, nearestPlayer.data)) {
                broadcast({
                    type: "esimio_game_command",
                    command: "caught",
                    player: nearestPlayer.id
                });
                console.log(`Esimio ${esimio.id} atrapó al jugador ${nearestPlayer.id}`);
            }
        } else {
            moveEsimioRandomly(esimio);
        }

        broadcast({
            type: "esimio_position",
            id: esimio.id,
            x: esimio.position.x,
            y: esimio.position.y,
            map: esimioGame.currentMap
        });
    });
}

function isValidEsimioMove(x, y) {
    if (x < 0 || x >= 40 || y < 0 || y >= 40) return false;

    const blockedAreas = [
        { xMin: 7, xMax: 14, yMin: 28, yMax: 29 },
        { xMin: 16, xMax: 17, yMin: 28, yMax: 29 },
        { xMin: 7, xMax: 14, yMin: 31, yMax: 32 },
        { xMin: 7, xMax: 14, yMin: 22, yMax: 23 },
        { xMin: 16, xMax: 17, yMin: 22, yMax: 23 },
        { xMin: 7, xMax: 14, yMin: 25, yMax: 26 },
        { xMin: 7, xMax: 14, yMin: 15, yMax: 16 },
        { xMin: 16, xMax: 17, yMin: 15, yMax: 16 },
        { xMin: 7, xMax: 14, yMin: 18, yMax: 19 },
        { xMin: 7, xMax: 14, yMin: 9, yMax: 10 },
        { xMin: 16, xMax: 17, yMin: 9, yMax: 10 },
        { xMin: 7, xMax: 14, yMin: 12, yMax: 13 },
        { xMin: 7, xMax: 14, yMin: 3, yMax: 4 },
        { xMin: 16, xMax: 17, yMin: 3, yMax: 4 },
        { xMin: 7, xMax: 14, yMin: 6, yMax: 7 },
        { xMin: 7, xMax: 38, yMin: 34, yMax: 38 },
        { xMin: 32, xMax: 38, yMin: 29, yMax: 38 },
        { xMin: 24, xMax: 29, yMin: 6, yMax: 18 },
        { xMin: 7, xMax: 38, yMin: 1, yMax: 4 }
    ];

    return !blockedAreas.some(area =>
        x >= area.xMin && x <= area.xMax && y >= area.yMin && y <= area.yMax
    );
}

function moveEsimioTowardsPlayer(esimio, playerData) {
    const currentX = esimio.position.x;
    const currentY = esimio.position.y;
    const dx = playerData.x - currentX;
    const dy = playerData.y - currentY;

    const possibleMoves = [];

    if (dx !== 0) {
        const newX = currentX + (dx > 0 ? 1 : -1);
        if (isValidEsimioMove(newX, currentY)) {
            possibleMoves.push({ x: newX, y: currentY });
        }
    }
    if (dy !== 0) {
        const newY = currentY + (dy > 0 ? 1 : -1);
        if (isValidEsimioMove(currentX, newY)) {
            possibleMoves.push({ x: currentX, y: newY });
        }
    }

    if (possibleMoves.length === 0) {
        const allMoves = [
            { x: currentX + 1, y: currentY },
            { x: currentX - 1, y: currentY },
            { x: currentX, y: currentY + 1 },
            { x: currentX, y: currentY - 1 }
        ];
        possibleMoves.push(...allMoves.filter(move => isValidEsimioMove(move.x, move.y)));
    }

    if (possibleMoves.length > 0) {
        if (esimioGame.difficulty >= 2) {
            possibleMoves.sort((a, b) => {
                const distA = calculateDistance(a.x, a.y, playerData.x, playerData.y);
                const distB = calculateDistance(b.x, b.y, playerData.x, playerData.y);
                return distA - distB;
            });
            esimio.position = possibleMoves[0];
        } else {
            esimio.position = possibleMoves[Math.floor(Math.random() * possibleMoves.length)];
        }
    }
}

function moveEsimioRandomly(esimio) {
    const currentX = esimio.position.x;
    const currentY = esimio.position.y;

    const possibleMoves = [
        { x: currentX, y: currentY - 1 },
        { x: currentX + 1, y: currentY },
        { x: currentX, y: currentY + 1 },
        { x: currentX - 1, y: currentY }
    ].filter(move => isValidEsimioMove(move.x, move.y));

    if (possibleMoves.length > 0) {
        esimio.position = possibleMoves[Math.floor(Math.random() * possibleMoves.length)];
    }
}

function isEsimioCaught(esimio, playerData) {
    const distance = calculateDistance(
        esimio.position.x, esimio.position.y,
        playerData.x, playerData.y
    );
    return distance <= 1.5;
}


// Función para procesar mensajes de esimios
function processEsimioGameMessages(message) {
    if (message.type === "esimio_game_update") {
        const action = message.action;

        switch (action) {
            case "start":
                if (!esimioGame.isActive) {
                    const difficulty = message.difficulty || 1;
                    const mapName = message.map || "esime";
                    startEsimioGame(difficulty, mapName);
                }
                break;

            case "stop":
                if (esimioGame.isActive) {
                    stopEsimioGame();
                }
                break;
        }
    }
}

// Rutas administrativas
app.post("/admin/esimio/start", (req, res) => {
    const difficulty = req.body.difficulty || 1;
    const mapName = req.body.map || "esime";
    startEsimioGame(difficulty, mapName);
    res.json({ message: "Juego de esimios iniciado", state: esimioGame });
});

app.post("/admin/esimio/stop", (req, res) => {
    stopEsimioGame();
    res.json({ message: "Juego de esimios detenido", state: esimioGame });
});

app.get("/admin/esimio/state", (req, res) => {
    res.json(esimioGame);
});

// Cleanup Prisma on shutdown
process.on('SIGINT', async () => {
    console.log('\nShutting down gracefully...');
    await prisma.$disconnect();
    process.exit(0);
});

process.on('SIGTERM', async () => {
    console.log('\nShutting down gracefully...');
    await prisma.$disconnect();
    process.exit(0);
});