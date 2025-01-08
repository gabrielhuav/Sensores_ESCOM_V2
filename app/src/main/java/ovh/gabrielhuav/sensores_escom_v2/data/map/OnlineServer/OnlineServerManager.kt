package ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer

import android.content.Context
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue

class OnlineServerManager(private val context: Context, private var listener: WebSocketListener? = null) {
    companion object {
        @Volatile
        private var INSTANCE: OnlineServerManager? = null

        fun getInstance(context: Context, listener: WebSocketListener? = null): OnlineServerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnlineServerManager(context, listener).also { INSTANCE = it }
            }
        }
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val messageQueue = LinkedBlockingQueue<String>()
    private var isConnected = false

    // Listeners para eventos de conexión
    private var onConnectionCompleteListener: (() -> Unit)? = null
    private var onConnectionFailedListener: (() -> Unit)? = null

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

    fun queueMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
        } else {
            messageQueue.offer(message) // Almacena los mensajes hasta que la conexión esté lista
        }
    }

    fun sendBothPositions(playerId: String, localX: Int, localY: Int, remoteX: Int?, remoteY: Int?, map: String) {
        val message = JSONObject().apply {
            put("type", "update")
            put("id", playerId)
            put("map", map)
            put("local", JSONObject().apply {
                put("x", localX)
                put("y", localY)
            })
            if (remoteX != null && remoteY != null) {
                put("remote", JSONObject().apply {
                    put("x", remoteX)
                    put("y", remoteY)
                })
            }
        }.toString()

        queueMessage(message)
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

    fun setOnConnectionCompleteListener(listener: () -> Unit) {
        this.onConnectionCompleteListener = listener
    }

    fun setOnConnectionFailedListener(listener: () -> Unit) {
        this.onConnectionFailedListener = listener
    }

    private inner class DefaultWebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("Connected to server")
            this@OnlineServerManager.webSocket = webSocket
            isConnected = true
            flushMessageQueue() // Envía los mensajes en cola
            onConnectionCompleteListener?.invoke() // Notificar que la conexión se completó
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("ServerReceived message: $text")
            listener?.onMessageReceived(text)  // Usar ?. en lugar de .
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
            onConnectionFailedListener?.invoke() // Notificar que la conexión falló
        }
    }
}
