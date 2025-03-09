const express = require("express");
const { WebSocketServer } = require("ws");
const path = require("path");

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
const zombieState = {
    position: { x: 30, y: 30 },
    target: null,
    isActive: false,
    updateInterval: 1000,
    currentMap: "escom_cafeteria",
    difficulty: 1
};

function initializeZombieState() {
    zombieState.position = { x: 30, y: 30 };
    zombieState.isActive = false;
    zombieState.updateInterval = 1000;
    zombieState.currentMap = "escom_cafeteria";
    zombieState.difficulty = 1;
    zombieState.target = null;
    console.log("Estado del zombie inicializado:", zombieState);
}

// Intervalo de actualización del zombie
let zombieUpdateInterval = null;

// Función para iniciar el minijuego del zombie
function startZombieGame(difficulty = 1) {
    zombieState.isActive = true;
    zombieState.difficulty = difficulty;
    zombieState.position = { x: 30, y: 30 };
    
    // Ajustar velocidad según dificultad
    zombieState.updateInterval = difficulty === 1 ? 1200 : (difficulty === 2 ? 800 : 500);
    
    // Notificar a todos los clientes que el juego ha iniciado
    broadcast({
        type: "zombie_game_command",
        command: "start",
        difficulty: difficulty
    });
    
    // AÑADIR: Enviar posición inicial del zombie a todos los clientes
    broadcast({
        type: "zombie_position",
        x: zombieState.position.x,
        y: zombieState.position.y,
        map: zombieState.currentMap
    });
    
    // Iniciar la actualización periódica del zombie
    startZombieUpdates();
    
    console.log(`Minijuego zombie iniciado con dificultad ${difficulty}`);
    console.log(`Posición inicial del zombie: (${zombieState.position.x}, ${zombieState.position.y})`);
}


// Función para detener el minijuego del zombie
function stopZombieGame() {
    zombieState.isActive = false;
    
    // Detener la actualización periódica
    if (zombieUpdateInterval) {
        clearInterval(zombieUpdateInterval);
        zombieUpdateInterval = null;
    }
    
    // Notificar a todos los clientes que el juego ha terminado
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
    
    // Crear nuevo intervalo
    zombieUpdateInterval = setInterval(() => {
        if (zombieState.isActive) {
            updateZombiePosition();
        } else {
            clearInterval(zombieUpdateInterval);
            zombieUpdateInterval = null;
        }
    }, zombieState.updateInterval);
}

// Función para actualizar la posición del zombie
function updateZombiePosition() {
    if (!zombieState.isActive) return;
    
    // Buscar jugadores en la cafetería para perseguir
    const playersInCafeteria = Object.entries(players).filter(([id, data]) => {
        return data.currentMap === "escom_cafeteria";
    });
    
    console.log(`Zombie update - Players in cafeteria: ${playersInCafeteria.length}`);
    
    if (playersInCafeteria.length > 0) {
        // Seleccionar un jugador aleatorio como objetivo si no hay uno definido
        if (!zombieState.target || Math.random() < 0.1) { // 10% de probabilidad de cambiar de objetivo
            const randomIndex = Math.floor(Math.random() * playersInCafeteria.length);
            zombieState.target = playersInCafeteria[randomIndex][0];
        }
        
        // Obtener posición del jugador objetivo
        const targetPlayer = players[zombieState.target];
        if (targetPlayer) {
            // Mover hacia el jugador
            const currentX = zombieState.position.x;
            const currentY = zombieState.position.y;
            const targetX = targetPlayer.x;
            const targetY = targetPlayer.y;
            
            // Calcular dirección
            const moveX = currentX < targetX ? 1 : (currentX > targetX ? -1 : 0);
            const moveY = currentY < targetY ? 1 : (currentY > targetY ? -1 : 0);
            
            // En dificultades más altas, el zombie puede moverse en diagonal
            if (zombieState.difficulty >= 2 && Math.random() > 0.3) {
                // Movimiento en ambas direcciones (diagonal)
                zombieState.position.x += moveX;
                zombieState.position.y += moveY;
            } else {
                // Movimiento en una sola dirección (horizontal o vertical)
                if (Math.random() > 0.5 && moveX !== 0) {
                    zombieState.position.x += moveX;
                } else if (moveY !== 0) {
                    zombieState.position.y += moveY;
                }
            }

            // Asegurarse que el mensaje se está enviando correctamente
            const zombieUpdateMessage = {
                type: "zombie_position",
                x: zombieState.position.x,
                y: zombieState.position.y,
                map: zombieState.currentMap
            };
            
            console.log("Enviando mensaje zombie:", JSON.stringify(zombieUpdateMessage));
            broadcast(zombieUpdateMessage);
            
            // Limitar a los límites del mapa
            zombieState.position.x = Math.max(0, Math.min(39, zombieState.position.x));
            zombieState.position.y = Math.max(0, Math.min(39, zombieState.position.y));
            
            // Enviar actualización a todos los clientes
            broadcast({
                type: "zombie_position",
                x: zombieState.position.x,
                y: zombieState.position.y,
                map: zombieState.currentMap
            });
            
            // Verificar si el zombie ha atrapado a algún jugador
            checkZombieCollisions();
        }
    }
}

const caughtPlayers = new Set();


// Verificar si el zombie ha atrapado a algún jugador
function checkZombieCollisions() {
    const catchDistance = 2; // Distancia para considerar que ha atrapado a un jugador
    
    Object.entries(players).forEach(([playerId, playerData]) => {
        if (playerData.currentMap === "escom_cafeteria" && !caughtPlayers.has(playerId)) {
            const distanceX = Math.abs(zombieState.position.x - playerData.x);
            const distanceY = Math.abs(zombieState.position.y - playerData.y);
            
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
        }
    });
}
// Procesar mensajes relacionados con el minijuego del zombie
function processZombieGameMessages(message) {
    if (message.type === "zombie_game_update") {
        const action = message.action;
        
        switch (action) {
            case "start":
                if (!zombieState.isActive) {
                    startZombieGame(1); // Dificultad inicial por defecto
                }
                break;
                
            case "stop":
                if (zombieState.isActive) {
                    stopZombieGame();
                }
                break;
                
            case "complete":
                // Un jugador ha completado el minijuego
                if (zombieState.isActive) {
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
        // Un jugador ha recogido comida
        broadcast({
            type: "zombie_game_food",
            player: message.player,
            score: message.score,
            x: message.x,
            y: message.y
        });
        
        // Reducir velocidad del zombie temporalmente
        const oldInterval = zombieState.updateInterval;
        zombieState.updateInterval += 300; // Hacer que sea más lento
        
        // Notificar ralentización
        broadcast({
            type: "zombie_game_command",
            command: "zombie_slowed",
            player: message.player
        });
        
        // Restaurar velocidad después de 3 segundos
        setTimeout(() => {
            zombieState.updateInterval = oldInterval;
            
            // Notificar a los clientes
            broadcast({
                type: "zombie_game_command",
                command: "zombie_speed_normal"
            });
        }, 3000);
    }
}

// Añadir procesamiento de mensajes para el minijuego zombie en la sección existente
// Buscar la sección donde se manejan los mensajes en el switch y añadir:

// En el evento "message" del WebSocket:
wss.on("connection", (ws) => {
    // [código existente]
    initializeZombieState();
    ws.on("message", (message) => {
        try {
            const data = JSON.parse(message);
            // [código existente]
            
            // Añadir esta parte al switch para procesar mensajes del minijuego
            switch (data.type) {
                // [casos existentes]
                
                case "zombie_game_update":
                case "zombie_game_food":
                    processZombieGameMessages(data);
                    break;
            }
            
        } catch (error) {
            // [código existente]
        }
    });
    
    // [código existente]
});

// Comandos administrativos para controlar el minijuego
// Puedes agregar un endpoint REST para controlar el juego desde fuera

app.post("/admin/zombie/start", (req, res) => {
    const difficulty = req.body.difficulty || 1;
    startZombieGame(difficulty);
    res.json({ message: "Minijuego zombie iniciado", state: zombieState });
});

app.post("/admin/zombie/stop", (req, res) => {
    stopZombieGame();
    res.json({ message: "Minijuego zombie detenido", state: zombieState });
});

app.get("/admin/zombie/state", (req, res) => {
    res.json(zombieState);
});

app.get("/admin/zombie/start-test", (req, res) => {
    startZombieGame(1);
    res.json({ message: "Minijuego zombie iniciado para pruebas", state: zombieState });
});

app.get("/admin/zombie/force-update", (req, res) => {
    if (zombieState.isActive) {
        // Forzar una actualización inmediata
        updateZombiePosition();
        res.json({ message: "Actualización del zombie forzada", position: zombieState.position });
    } else {
        res.json({ message: "El zombie no está activo actualmente" });
    }
});