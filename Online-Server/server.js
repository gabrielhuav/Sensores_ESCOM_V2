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