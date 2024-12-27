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

// Estructura de datos para almacenar jugadores y sus coordenadas por mapa
const players = {};

// Función para enviar mensajes a todos los clientes conectados
function broadcast(data) {
  const message = JSON.stringify(data);
  wss.clients.forEach((client) => {
    if (client.readyState === client.OPEN) {
      client.send(message);
    }
  });
}

// Función auxiliar para procesar las coordenadas
function processPosition(data) {
  // Si viene en formato {x, y} directo
  if (typeof data.x === "number" && typeof data.y === "number") {
    return { x: data.x, y: data.y };
  }
  // Si viene dentro de un objeto local
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

        if (data.type === "join") {
            const trimmedId = data.id.trim();
            if (!players[trimmedId]) {
                players[trimmedId] = { positions: {}, currentMap: "main" };
                console.log(`Player joined: ${trimmedId}`);
            }
        } else if (data.type === "update") {
            const trimmedId = data.id.trim();
            const position = processPosition(data);

            if (players[trimmedId]) {
                const currentMap = data.map || players[trimmedId].currentMap;

                // Evitar sobrescribir si la posición no cambió
                const previousPosition = players[trimmedId].positions[currentMap] || {};
                if (
                    position &&
                    (position.x !== previousPosition.x || position.y !== previousPosition.y)
                ) {
                    players[trimmedId].positions[currentMap] = position;
                    players[trimmedId].currentMap = currentMap;
                    console.log(`Updated position for ${trimmedId}:`, position);
                }

                // Procesar posiciones remotas
                if (data.remotes && Array.isArray(data.remotes)) {
                    data.remotes.forEach((remote) => {
                        if (remote.id && typeof remote.x === "number" && typeof remote.y === "number") {
                            const remoteId = remote.id.trim();
                            if (!players[remoteId]) {
                                players[remoteId] = { positions: {}, currentMap };
                            }

                            const remotePreviousPosition = players[remoteId].positions[currentMap] || {};
                            if (
                                remote.x !== remotePreviousPosition.x ||
                                remote.y !== remotePreviousPosition.y
                            ) {
                                players[remoteId].positions[currentMap] = { x: remote.x, y: remote.y };
                                players[remoteId].currentMap = currentMap;
                                console.log(`Updated remote position for ${remoteId}:`, {
                                    x: remote.x,
                                    y: remote.y,
                                });
                            }
                        }
                    });
                }
            } else {
                console.warn(`Player ${trimmedId} not found, creating new entry`);
                players[trimmedId] = { positions: { [data.map]: position }, currentMap: data.map };
            }
        } else if (data.type === "leave") {
            const trimmedId = data.id.trim();
            if (players[trimmedId]) {
                console.log(`Player left: ${trimmedId}`);
                delete players[trimmedId];
                delete players[`${trimmedId}_remote`];
            }
        }

        // Preparar y enviar actualizaciones a todos los clientes
        const broadcastData = {
            type: "positions",
            players: Object.fromEntries(
                Object.entries(players).map(([id, playerData]) => [
                    id,
                    {
                        currentMap: playerData.currentMap,
                        x: playerData.positions[playerData.currentMap]?.x || 0,
                        y: playerData.positions[playerData.currentMap]?.y || 0,
                    },
                ])
            ),
        };

        broadcast(broadcastData);
    } catch (error) {
        console.error("Error processing message:", error);
        console.error(error.stack);
    }
});


  ws.on("close", () => {
    console.log("A player disconnected");
    broadcast({ type: "positions", players });
  });
});

// Endpoint HTTP para mostrar el estado del servidor
app.get("/", (req, res) => {
  res.json({
    message: "WebSocket server is running. Connect to synchronize positions.",
    players: Object.fromEntries(
      Object.entries(players).map(([id, playerData]) => [
        id,
        {
          currentMap: playerData.currentMap,
          positions: playerData.positions,
        },
      ])
    ),
  });
});

app.get("/documentation", (req, res) => {
  res.sendFile(path.join(__dirname, "documentation.html"));
});