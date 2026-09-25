package com.example.myplayer.sync.transport

import android.util.Log
import com.example.myplayer.sync.model.SyncMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketSyncServer @Inject constructor() {

    companion object {
        private const val TAG = "SocketSyncServer"
        const val DEFAULT_PORT = 48950
        const val MAX_SLAVES = 4
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class ConnectedClient(
        var deviceId: String,
        val socket: Socket,
        val writer: BufferedWriter
    )

    // Maps socket remote address or deviceId to ConnectedClient
    private val clients = ConcurrentHashMap<String, ConnectedClient>()

    private val _incomingMessages = MutableSharedFlow<Pair<String, SyncMessage>>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Pair<String, SyncMessage>> = _incomingMessages.asSharedFlow()

    private val _clientDisconnects = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val clientDisconnects: SharedFlow<String> = _clientDisconnects.asSharedFlow()

    fun isRunning(): Boolean = serverSocket?.isClosed == false

    /**
     * Starts listening for Slave connections on the specified or next available port.
     * Returns the actual bound local port.
     */
    fun start(port: Int = DEFAULT_PORT): Int {
        stop()

        var boundPort = port
        var ss: ServerSocket? = null
        try {
            ss = ServerSocket(boundPort)
        } catch (e: Exception) {
            Log.w(TAG, "Port $port unavailable, selecting system-assigned port: ${e.message}")
            ss = ServerSocket(0)
            boundPort = ss.localPort
        }
        serverSocket = ss

        Log.i(TAG, "Server listening on port $boundPort")

        serverJob = scope.launch {
            while (isActive && serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    if (clients.size >= MAX_SLAVES) {
                        Log.w(TAG, "Max slaves ($MAX_SLAVES) reached, rejecting connection")
                        try { clientSocket.close() } catch (_: Exception) {}
                        continue
                    }
                    val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                    launchClientHandler(clientId, clientSocket)
                } catch (e: Exception) {
                    if (isActive && serverSocket?.isClosed == false) {
                        Log.e(TAG, "Error accepting client: ${e.message}")
                    }
                    break
                }
            }
        }

        return boundPort
    }

    private fun launchClientHandler(clientId: String, socket: Socket) {
        scope.launch {
            var identifiedDeviceId: String? = null
            try {
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

                val client = ConnectedClient(
                    deviceId = clientId,
                    socket = socket,
                    writer = writer
                )
                clients[clientId] = client
                Log.d(TAG, "Client connected: $clientId (total=${clients.size})")

                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break // EOF
                    if (line.isBlank()) continue
                    try {
                        val message = json.decodeFromString<SyncMessage>(line)
                        if (identifiedDeviceId == null && message.senderDeviceId.isNotBlank()) {
                            identifiedDeviceId = message.senderDeviceId
                            client.deviceId = identifiedDeviceId
                            clients[identifiedDeviceId] = client
                        }
                        _incomingMessages.emit(client.deviceId to message)
                    } catch (e: Exception) {
                        Log.w(TAG, "Malformed message from $clientId: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Client handler exception for $clientId: ${e.message}")
            } finally {
                clients.remove(clientId)
                identifiedDeviceId?.let { clients.remove(it) }
                try { socket.close() } catch (_: Exception) {}
                val finalId = identifiedDeviceId ?: clientId
                Log.d(TAG, "Client disconnected: $finalId (remaining=${clients.size})")
                _clientDisconnects.emit(finalId)
            }
        }
    }

    /**
     * Broadcasts a SyncMessage to all connected Slaves.
     */
    fun broadcast(message: SyncMessage) {
        val payload = try {
            json.encodeToString(message) + "\n"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode message for broadcast: ${e.message}")
            return
        }

        scope.launch {
            clients.values.distinctBy { it.socket }.forEach { client ->
                try {
                    synchronized(client.writer) {
                        client.writer.write(payload)
                        client.writer.flush()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send to client ${client.deviceId}: ${e.message}")
                }
            }
        }
    }

    /**
     * Sends a SyncMessage to a specific device.
     */
    fun sendTo(deviceId: String, message: SyncMessage) {
        val client = clients[deviceId] ?: return
        val payload = try {
            json.encodeToString(message) + "\n"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode message for $deviceId: ${e.message}")
            return
        }

        scope.launch {
            try {
                synchronized(client.writer) {
                    client.writer.write(payload)
                    client.writer.flush()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send message to $deviceId: ${e.message}")
            }
        }
    }

    fun getConnectedDeviceIds(): List<String> = clients.values.map { it.deviceId }.distinct()

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        clients.values.distinctBy { it.socket }.forEach { client ->
            try { client.socket.close() } catch (_: Exception) {}
        }
        clients.clear()
        Log.i(TAG, "Socket server stopped")
    }
}
