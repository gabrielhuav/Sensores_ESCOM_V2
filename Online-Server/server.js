Online-Server\server.js
@@ -1,55 +1,3 @@
/**
 * WebSocket Server for Player Synchronization
 *
 * This server allows players to connect via WebSocket, update their positions in real-time,
 * and receive updates about other players' positions.
 *
 * Technologies Used:
 * - Node.js
 * - Express.js
 * - WebSocket (via the `ws` library)
 *
 * Features:
 * 1. Handles player connections and disconnections.
 * 2. Synchronizes player positions across all connected clients.
 * 3. Provides a basic HTTP route for server status.
 *
 * Setup Instructions:
 * 1. Ensure Node.js is installed.
 * 2. Install dependencies using `npm install express ws`.
 * 3. Start the server using `node <filename>.js`.
 *
 * WebSocket Message Types:
 * - Incoming Messages:
 *   1. `join`: A new player joins the server.
 *      Example:
 *      {
 *          "type": "join",
 *          "id": "player1"
 *      }
 *   2. `update`: A player updates their position.
 *      Example:
 *      {
 *          "type": "update",
 *          "id": "player1",
 *          "x": 100,
 *          "y": 200
 *      }
 *
 * - Outgoing Messages:
 *   1. `positions`: The server broadcasts the updated positions of all players.
 *      Example:
 *      {
 *          "type": "positions",
 *          "players": {
 *              "player1": { "x": 100, "y": 200 },
 *              "player2": { "x": 50, "y": 75 }
 *          }
 *      }
 *
 * API Endpoints:
 * - `GET /`: Returns a simple status message indicating the server is running.
 */
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

// Función para generar un color aleatorio en formato hexadecimal
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
    if (typeof data.x === "number" && typeof data.y === "number") {
        return { x: data.x, y: data.y };
    }
    if (data.local && typeof data.local.x === "number" && typeof data.local.y === "number") {
        return { x: data.local.x, y: data.local.y };
    }
    return null;
}

wss.on("connection", (ws) => {
    console.log("A player connected");

    ws.on("message", (message) => {
        try {
            const data = JSON.parse(message);
            console.log("Received data:", data);

            const trimmedId = data.id?.trim();
            if (!trimmedId) return;

            switch (data.type) {
                case "join":
                    if (!players[trimmedId]) {
                        players[trimmedId] = {
                            x: 0,
                            y: 0,
                            currentMap: "main",
                            color: generateRandomColor(),
                            type: "local"
                        };
                        console.log(`Player joined: ${trimmedId}`);
                    }
                    break;

                case "update":
                    if (shouldUpdate(trimmedId)) {
                        const position = processPosition(data);
                        const currentMap = data.map || "main";

                        if (position) {
                            const previousPosition = players[trimmedId];

                            if (hasPositionChangedSignificantly(previousPosition, position)) {
                                // Actualizar jugador local
                                players[trimmedId] = {
                                    x: position.x,
                                    y: position.y,
                                    currentMap: currentMap,
                                    color: players[trimmedId]?.color || generateRandomColor(),
                                    type: "local"
                                };

                                // Procesar posiciones remotas si existen
                                if (data.remote) {
                                    const remoteId = `${trimmedId}_remote`;
                                    players[remoteId] = {
                                        x: data.remote.x,
                                        y: data.remote.y,
                                        currentMap: currentMap,
                                        color: "#FF0000", // Color fijo para remotos
                                        type: "remote"
                                    };
                                }

                                // Broadcast a todos los clientes
                                const broadcastData = {
                                    type: "positions",
                                    players: Object.fromEntries(
                                        Object.entries(players).map(([id, player]) => [
                                            id,
                                            {
                                                x: player.x,
                                                y: player.y,
                                                currentMap: player.currentMap,
                                                color: player.color,
                                                type: player.type
                                            }
                                        ])
                                    )
                                };
                                broadcast(broadcastData);
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
        broadcast({ 
            type: "positions",
            players: players
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
        if (currentTime - lastUpdateTime[playerId] > 30000) {
            console.log(`Removing inactive player: ${playerId}`);
            delete players[playerId];
            delete players[`${playerId}_remote`];
            delete lastUpdateTime[playerId];
            broadcast({
                type: "player_timeout",
                playerId: playerId
            });
        }
    });
}, 10000);
