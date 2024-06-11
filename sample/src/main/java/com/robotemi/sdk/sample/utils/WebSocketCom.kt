package com.robotemi.sdk.sample.utils

import android.util.Log
import com.neovisionaries.ws.client.*
import kotlinx.coroutines.*

open class WebSocketCom(address: String, timeout: Int) {
    private var ws: WebSocket = createWebSocket(address, timeout)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var ws_connected:Boolean = false

    init {
        // Try COnnecting for the First TIme
        connectWebSocket()
        // Check the Connection Status
        connectionStatusCheck()
    }

    private fun createWebSocket(serverUri: String, timeout: Int): WebSocket {
        return WebSocketFactory().createSocket(serverUri, timeout).apply {
            addListener(object : WebSocketAdapter() {
                override fun onTextMessage(websocket: WebSocket?, message: String?) {
                    Log.d("WebSocket", "Message received: $message")
                    onCommand(message!!)
                }

                override fun onConnected(websocket: WebSocket?, headers: Map<String, List<String>>?) {
                    Log.d("WebSocket", "CONNECTED HURRA")
                    ws_connected = true
                }

                override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
                    Log.d("WebSocket", "Connection error: ${exception?.message}")
                    ws_connected = false
                }

                override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
                    Log.d("WebSocket", "Disconnected from server, closedByServer: $closedByServer")
                    ws_connected = false
                }
            }).addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
        }
    }

    private fun connectWebSocket() {
        ws.connectAsynchronously()
    }

    fun connectionStatusCheck() {
        coroutineScope.launch {
            while (isActive) {  // Continues until the coroutine is cancelled
                if (ws_connected) {
                    // Perform some action if the variable is true

                } else {
                    // Perform another action if the variable is false
                    ws = ws.recreate().connectAsynchronously()
                }
                delay(5000)  // Wait for 5 seconds before checking again
            }
        }
    }

    fun getWebSocket(): WebSocket
    {
        return ws
    }

    fun isConnected(): Boolean
    {
        return ws_connected
    }

    fun close() {
        coroutineScope.cancel()
        ws.disconnect()
    }

    open fun onCommand(msg:String) {
    // Function to be called from on text message, and its functionality has to be
    // according the given task or implementation.
        Log.d("WebSocket", "Command Received: $msg")
    }
}