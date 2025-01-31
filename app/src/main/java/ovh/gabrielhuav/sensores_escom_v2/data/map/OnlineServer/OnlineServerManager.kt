package ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer

import android.content.Context
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue

class OnlineServerManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: OnlineServerManager? = null

        fun getInstance(context: Context): OnlineServerManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnlineServerManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val messageQueue = LinkedBlockingQueue<String>()
    private var isConnected = false
    private var currentUrl: String? = null
    private var listener: WebSocketListener? = null

    fun setListener(listener: WebSocketListener) {
        this.listener = listener
    }

    private var onConnectionCompleteListener: (() -> Unit)? = null
    private var onConnectionFailedListener: (() -> Unit)? = null

    fun connectToServer(url: String) {
        // Evitar reconexión si ya está conectado a la misma URL
        if (isConnected && url == currentUrl) return

        // Cerrar conexión existente si hay
        webSocket?.close(1000, "Reconnecting")

        currentUrl = url
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, DefaultWebSocketListener())
    }

    fun isConnected() = isConnected

    fun disconnectFromServer() {
        webSocket?.close(1000, "Client disconnected")
        isConnected = false
        currentUrl = null
    }

    fun sendJoinMessage(playerId: String) {
        val message = """{"type": "join", "id": "$playerId"}"""
        queueMessage(message)
    }

    fun sendUpdateMessage(playerId: String, x: Int, y: Int, map: String) {
        val message = """{"type": "update", "id": "$playerId", "x": $x, "y": $y, "map": "$map"}"""
        queueMessage(message)
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

    fun queueMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
        } else {
            messageQueue.offer(message)
        }
    }

    private fun flushMessageQueue() {
        while (messageQueue.isNotEmpty() && isConnected) {
            messageQueue.poll()?.let { message ->
                webSocket?.send(message)
            }
        }
    }

    interface WebSocketListener {
        fun onMessageReceived(message: String)
    }

    fun setOnConnectionCompleteListener(listener: () -> Unit) {
        onConnectionCompleteListener = listener
    }

    fun setOnConnectionFailedListener(listener: () -> Unit) {
        onConnectionFailedListener = listener
    }

    private inner class DefaultWebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("Connected to server")
            this@OnlineServerManager.webSocket = webSocket
            isConnected = true
            flushMessageQueue()
            onConnectionCompleteListener?.invoke()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("ServerReceived message: $text")
            listener?.onMessageReceived(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("Received bytes: $bytes")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("Closing: $code / $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("Closed: $code / $reason")
            isConnected = false
            if (code != 1000) {
                onConnectionFailedListener?.invoke()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Error: ${t.message}")
            isConnected = false
            onConnectionFailedListener?.invoke()
        }
    }
}
