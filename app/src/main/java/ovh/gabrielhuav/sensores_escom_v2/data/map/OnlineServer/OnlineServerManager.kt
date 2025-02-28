package ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer

import android.content.Context
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

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

    // Método para verificar si el WebSocket está conectado
    fun isWebSocketConnected(): Boolean {
        return webSocket != null
    }

    fun sendJoinMessage(playerId: String) {
        val message = """{"type": "join", "id": "$playerId"}"""
        queueMessage(message)
    }

    fun sendUpdateMessage(playerId: String, x: Int, y: Int, map: String) {
        val message = JSONObject().apply {
            put("type", "update")
            put("id", playerId)
            put("x", x)
            put("y", y)
            put("map", map)
        }.toString()
        queueMessage(message)
    }

    fun sendBothPositions(playerId: String, localX: Int, localY: Int, remoteX: Int?, remoteY: Int?, map: String) {
        // Solo enviar la posición que cambió, no ambas
        if (remoteX != null && remoteY != null) {
            // Si es una actualización remota
            val message = JSONObject().apply {
                put("type", "update")
                put("id", playerId)
                put("x", remoteX)
                put("y", remoteY)
                put("map", map)
            }.toString()
            queueMessage(message)
        } else {
            // Si es una actualización local
            val message = JSONObject().apply {
                put("type", "update")
                put("id", playerId)
                put("x", localX)
                put("y", localY)
                put("map", map)
            }.toString()
            queueMessage(message)
        }
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
            // Ejecutar en el hilo principal
            mainHandler.post {
                onConnectionCompleteListener?.invoke()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("ServerReceived message: $text")
            // Entregar mensaje al listener en el hilo principal
            mainHandler.post {
                listener?.onMessageReceived(text)
            }
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
            // Ejecutar en el hilo principal
            if (code != 1000) {
                mainHandler.post {
                    onConnectionFailedListener?.invoke()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Error: ${t.message}")
            isConnected = false
            // Ejecutar en el hilo principal
            mainHandler.post {
                onConnectionFailedListener?.invoke()
            }
        }
    }

    fun requestPositionsUpdate() {
        webSocket?.send(JSONObject().apply {
            put("type", "request_positions")
        }.toString())
    }
}