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

wss.on("connection", (ws) => {
  console.log("A player connected");

  ws.on("message", (message) => {
    try {
      const data = JSON.parse(message);

      if (data.type === "join") {
        // Asegurar que el jugador tenga un ID único y no se sobrescriba
        if (!players[data.id]) {
          players[data.id] = { positions: {}, currentMap: "main" }; // Estructura para múltiples mapas
        }

        console.log(`Player joined: ${data.id}`);
      } else if (data.type === "update") {
        if (players[data.id]) {
          // Actualizar las coordenadas del jugador en el mapa actual
          players[data.id].positions[data.map] = { x: data.x, y: data.y };
          players[data.id].currentMap = data.map;
        }
      } else if (data.type === "leave") {
        if (players[data.id]) {
          console.log(`Player left: ${data.id}`);
          delete players[data.id];
        }
      }

      // Preparar un paquete de posiciones para transmitir a los clientes
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
    }
  });

  ws.on("close", () => {
    console.log("A player disconnected");
    // No podemos identificar el ID del jugador desconectado sin más información
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
