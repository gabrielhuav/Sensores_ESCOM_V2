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
const RECONNECT_TIMEOUT = 5000; // Tiempo para mantener la información del jugador después de desconexión

const players = {};
const lastUpdateTime = {};
const disconnectedPlayers = new Map(); // Para manejar reconexiones

function generateRandomColor() {
    return '#' + Math.floor(Math.random()*16777215).toString(16);
}

function broadcast(data, excludeWs = null) {
    const message = JSON.stringify(data);
    wss.clients.forEach((client) => {
        if (client !== excludeWs && client.readyState === client.OPEN) {
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
    if (typeof data.x === "number" && typeof data.y === "number") {
        return { x: data.x, y: data.y };
    }
    if (data.local && typeof data.local.x === "number" && typeof data.local.y === "number") {
        return { x: data.local.x, y: data.local.y };
    }
    return null;
}

function handlePlayerDisconnection(playerId) {
    if (players[playerId]) {
        // Guardar el estado del jugador
        disconnectedPlayers.set(playerId, {
            data: players[playerId],
            timestamp: Date.now()
        });

        // Programar la eliminación después del timeout
        setTimeout(() => {
            if (disconnectedPlayers.has(playerId)) {
                disconnectedPlayers.delete(playerId);
                delete players[playerId];
                delete players[`${playerId}_remote`];
                delete lastUpdateTime[playerId];
                broadcast({
                    type: "player_timeout",
                    playerId: playerId
                });
            }
        }, RECONNECT_TIMEOUT);
    }
}

wss.on("connection", (ws) => {
    console.log("A player connected");
    ws.isAlive = true;

    ws.on("pong", () => {
        ws.isAlive = true;
    });

    ws.on("message", (message) => {
        try {
            const data = JSON.parse(message);
            console.log("Received data:", data);

            const trimmedId = data.id?.trim();
            if (!trimmedId) return;

            ws.playerId = trimmedId; // Asociar el ID del jugador con la conexión

            switch (data.type) {
                case "join":
                    // Verificar si el jugador estaba desconectado recientemente
                    const disconnectedPlayer = disconnectedPlayers.get(trimmedId);
                    if (disconnectedPlayer) {
                        players[trimmedId] = disconnectedPlayer.data;
                        disconnectedPlayers.delete(trimmedId);
                    } else if (!players[trimmedId]) {
                        players[trimmedId] = {
                            x: 0,
                            y: 0,
                            currentMap: "main",
                            color: generateRandomColor(),
                            type: "local"
                        };
                    }
                    console.log(`Player joined/reconnected: ${trimmedId}`);
                    
                    // Enviar estado actual a todos
                    broadcast({
                        type: "positions",
                        players: players
                    });
                    break;

                case "update":
                    if (shouldUpdate(trimmedId)) {
                        const position = processPosition(data);
                        const currentMap = data.map || "main";

                        if (position) {
                            const previousPosition = players[trimmedId];

                            if (hasPositionChangedSignificantly(previousPosition, position)) {
                                players[trimmedId] = {
                                    x: position.x,
                                    y: position.y,
                                    currentMap: currentMap,
                                    color: players[trimmedId]?.color || generateRandomColor(),
                                    type: "local"
                                };

                                if (data.remote) {
                                    const remoteId = `${trimmedId}_remote`;
                                    players[remoteId] = {
                                        x: data.remote.x,
                                        y: data.remote.y,
                                        currentMap: currentMap,
                                        color: "#FF0000",
                                        type: "remote"
                                    };
                                }

                                broadcast({
                                    type: "positions",
                                    players: players
                                });
                            }
                        }
                    }
                    break;

                case "leave":
                    handlePlayerDisconnection(trimmedId);
                    break;
            }

        } catch (error) {
            console.error("Error processing message:", error);
            console.error(error.stack);
        }
    });

    ws.on("close", () => {
        if (ws.playerId) {
            handlePlayerDisconnection(ws.playerId);
        }
        console.log("A player disconnected");
    });
});

// Ping para mantener conexiones activas
const interval = setInterval(() => {
    wss.clients.forEach((ws) => {
        if (ws.isAlive === false) {
            if (ws.playerId) {
                handlePlayerDisconnection(ws.playerId);
            }
            return ws.terminate();
        }
        ws.isAlive = false;
        ws.ping();
    });
}, 30000);

wss.on("close", () => {
    clearInterval(interval);
});

app.get("/", (req, res) => {
    res.json({
        message: "WebSocket server is running.",
        connectedPlayers: Object.keys(players).length,
        players: players
    });
});
