// server.js
// Archivo principal del servidor WebSocket
const express = require("express");
const { WebSocketServer } = require("ws");
const path = require("path");

// Importar módulos para el juego de zombies
const {
    isValidMove,
    calculateDistance,
    moveZombieTowardsPlayer,
    moveZombieRandomly,
    isPlayerCaught
} = require('./zombieController');

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

            // ⬇️ AÑADE ESTE NUEVO CASE AQUÍ ⬇️
            case "esimio_game_update":
                processEsimioGameMessages(data);
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