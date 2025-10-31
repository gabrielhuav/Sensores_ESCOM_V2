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