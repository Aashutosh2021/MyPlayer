package com.example.myplayer.sync.transport

import android.util.Log
import com.example.myplayer.sync.model.SyncMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketSyncClient @Inject constructor() : SyncTransport {

    companion object {
        private const val TAG = "SocketSyncClient"
        private const val CONNECT_TIMEOUT_MS = 5000
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<SyncMessage>(extraBufferCapacity = 64)

    override fun observeMessages(): Flow<SyncMessage> = _messages.asSharedFlow()

    override fun isConnected(): Boolean = socket?.isConnected == true && socket?.isClosed == false

    suspend fun connect(host: String, port: Int) {
        withContext(Dispatchers.IO) {
            disconnect()

            val s = Socket()
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket = s

            writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))

            Log.i(TAG, "Connected to Master at $host:$port")

            clientJob = scope.launch {
                try {
                    while (isActive && !s.isClosed) {
                        val line = reader.readLine() ?: break // EOF from server
                        if (line.isBlank()) continue
                        try {
                            val message = json.decodeFromString<SyncMessage>(line)
                            _messages.emit(message)
                        } catch (e: Exception) {
                            Log.w(TAG, "Malformed message from server: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.d(TAG, "Read loop terminated: ${e.message}")
                    }
                } finally {
                    disconnect()
                }
            }
        }
    }

    override suspend fun send(message: SyncMessage) {
        withContext(Dispatchers.IO) {
            val w = writer ?: run {
                Log.w(TAG, "Cannot send message: not connected")
                return@withContext
            }
            try {
                val payload = json.encodeToString(message) + "\n"
                synchronized(w) {
                    w.write(payload)
                    w.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message: ${e.message}")
                disconnect()
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            clientJob?.cancel()
            clientJob = null
            try {
                socket?.close()
            } catch (_: Exception) {}
            socket = null
            writer = null
            Log.i(TAG, "Disconnected from Master")
        }
    }
}
