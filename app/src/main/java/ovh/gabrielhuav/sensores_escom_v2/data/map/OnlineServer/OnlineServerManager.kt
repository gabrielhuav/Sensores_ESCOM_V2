package ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer

import okhttp3.*
import okio.ByteString
import java.util.concurrent.LinkedBlockingQueue

class OnlineServerManager(private val listener: WebSocketListener) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val messageQueue = LinkedBlockingQueue<String>()
    private var isConnected = false

    fun connectToServer(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, DefaultWebSocketListener())
        client.dispatcher.executorService.shutdown() // Cierra los hilos después de conectarse
    }

    fun disconnectFromServer() {
        webSocket?.close(1000, "Client disconnected")
        isConnected = false
    }

    fun sendJoinMessage(playerId: String) {
        val message = """{"type": "join", "id": "$playerId"}"""
        queueMessage(message)
    }

    fun sendUpdateMessage(playerId: String, x: Int, y: Int, map: String) {
        val message = """{"type": "update", "id": "$playerId", "x": $x, "y": $y, "map": "$map"}"""
        queueMessage(message)
    }

    private fun queueMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
        } else {
            messageQueue.offer(message) // Almacena los mensajes hasta que la conexión esté lista
        }
    }

    private fun flushMessageQueue() {
        while (messageQueue.isNotEmpty() && isConnected) {
            val message = messageQueue.poll()
            webSocket?.send(message)
        }
    }

    interface WebSocketListener {
        fun onMessageReceived(message: String)
    }

    private inner class DefaultWebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("Connected to server")
            this@OnlineServerManager.webSocket = webSocket
            isConnected = true
            flushMessageQueue() // Envía los mensajes en cola
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
            isConnected = false
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("Closed: $code / $reason")
            isConnected = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Error: ${t.message}")
            isConnected = false
        }
    }
}
