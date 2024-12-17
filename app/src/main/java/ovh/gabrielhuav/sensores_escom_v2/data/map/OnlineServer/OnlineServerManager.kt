package ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer

import okhttp3.*
import okio.ByteString

class OnlineServerManager(private val listener: WebSocketListener) {

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket

    fun connectToServer(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, DefaultWebSocketListener())
    }

    fun disconnectFromServer() {
        webSocket.close(1000, "Client disconnected")
    }

    fun sendJoinMessage(playerId: String) {
        val message = """{"type": "join", "id": "$playerId"}"""
        webSocket.send(message)
    }

    fun sendUpdateMessage(playerId: String, x: Int, y: Int) {
        val message = """{"type": "update", "id": "$playerId", "x": $x, "y": $y}"""
        webSocket.send(message)
    }

    interface WebSocketListener {
        fun onMessageReceived(message: String)
    }

    inner class DefaultWebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("Connected to server")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("Received message: $text")
            listener.onMessageReceived(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("Received bytes: $bytes")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("Closing: $code / $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("Closed: $code / $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Error: ${t.message}")
        }
    }
}