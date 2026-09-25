package com.example.myplayer.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

private const val TAG = "SyncTransport"
const val SYNC_PORT_RANGE_START = 45200
const val SYNC_PORT_RANGE_END = 45250
private const val CONNECT_TIMEOUT_MS = 2500

sealed class TransportEvent {
    data class PortBound(val port: Int) : TransportEvent()
    object PeerConnected : TransportEvent()
    data class Message(val message: SyncMessage) : TransportEvent()
    object Disconnected : TransportEvent()
    data class Error(val throwable: Throwable) : TransportEvent()
}

interface SyncTransport {
    fun events(): Flow<TransportEvent>
    suspend fun send(message: SyncMessage): Boolean
    fun close()
}

/** Master side — binds a free port and continuously accepts Slave connections (supports N concurrent Slaves). */
class LanServerTransport : SyncTransport {

    private var serverSocket: ServerSocket? = null
    private val clientSockets = CopyOnWriteArrayList<Socket>()
    private val clientWriters = CopyOnWriteArrayList<OutputStream>()
    private val transportScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun events(): Flow<TransportEvent> = callbackFlow {
        var boundPort = -1
        var lastError: IOException? = null
        for (candidatePort in SYNC_PORT_RANGE_START..SYNC_PORT_RANGE_END) {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress("0.0.0.0", candidatePort))
                serverSocket = ss
                boundPort = candidatePort
                Log.d(TAG, "LanServerTransport successfully bound to 0.0.0.0:$boundPort")
                break
            } catch (e: IOException) {
                lastError = e
            }
        }
        if (boundPort == -1) {
            trySend(TransportEvent.Error(lastError ?: IOException("No free port in $SYNC_PORT_RANGE_START..$SYNC_PORT_RANGE_END")))
            close()
            return@callbackFlow
        }
        trySend(TransportEvent.PortBound(boundPort))

        // Non-blocking accept loop: accepts multiple concurrent Slaves
        try {
            while (serverSocket?.isClosed == false) {
                Log.d(TAG, "Server waiting for incoming client connection on port $boundPort...")
                val socket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    if (serverSocket?.isClosed != true) {
                        Log.w(TAG, "Server accept failed: ${e.message}")
                    }
                    break
                } ?: break

                Log.d(TAG, "Accepted client connection from ${socket.inetAddress?.hostAddress}:${socket.port}")
                socket.tcpNoDelay = true
                val writer = try {
                    socket.getOutputStream()
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to get socket output stream: ${e.message}")
                    try { socket.close() } catch (_: Exception) {}
                    continue
                }

                clientSockets.add(socket)
                clientWriters.add(writer)
                trySend(TransportEvent.PeerConnected)

                // Launch per-client read loop in background coroutine so accept() is never blocked
                transportScope.launch {
                    try {
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                        while (!socket.isClosed) {
                            val line = try {
                                reader.readLine()
                            } catch (e: IOException) {
                                null
                            } ?: break

                            val message = SyncMessageCodec.decode(line)
                            if (message != null) {
                                Log.d(TAG, "Server received message: ${message.javaClass.simpleName} from ${message.senderId}")
                                trySend(TransportEvent.Message(message))
                            } else {
                                Log.w(TAG, "Server dropped malformed message line")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Client read loop error or closed: ${e.message}")
                    } finally {
                        Log.d(TAG, "Client socket disconnected: ${socket.inetAddress?.hostAddress}:${socket.port}")
                        clientWriters.remove(writer)
                        clientSockets.remove(socket)
                        try { socket.close() } catch (_: Exception) {}
                        if (clientSockets.isEmpty()) {
                            trySend(TransportEvent.Disconnected)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (serverSocket?.isClosed != true) {
                trySend(TransportEvent.Error(e))
            }
        }

        awaitClose { closeInternal() }
    }.flowOn(Dispatchers.IO)

    override suspend fun send(message: SyncMessage): Boolean = withContext(Dispatchers.IO) {
        if (clientWriters.isEmpty()) {
            Log.w(TAG, "Server send failed: no active client writers")
            return@withContext false
        }
        val encoded = SyncMessageCodec.encode(message) + "\n"
        val bytes = encoded.toByteArray(Charsets.UTF_8)
        Log.d(TAG, "Server broadcasting to ${clientWriters.size} client(s): ${message.javaClass.simpleName}")
        var anySuccess = false
        val iterator = clientWriters.iterator()
        while (iterator.hasNext()) {
            val out = iterator.next()
            try {
                out.write(bytes)
                out.flush()
                anySuccess = true
            } catch (e: IOException) {
                Log.w(TAG, "Server error writing to client writer: ${e.message}")
            }
        }
        anySuccess
    }

    override fun close() = closeInternal()

    private fun closeInternal() {
        transportScope.cancel()
        for (s in clientSockets) {
            try { s.close() } catch (_: IOException) {}
        }
        clientSockets.clear()
        clientWriters.clear()
        try { serverSocket?.close() } catch (_: IOException) {}
        serverSocket = null
    }
}

/** Slave side — connects to the Master's resolved host:port. */
class LanClientTransport(
    private val host: InetAddress,
    private val port: Int
) : SyncTransport {

    private var socket: Socket? = null
    @Volatile private var writer: OutputStream? = null

    override fun events(): Flow<TransportEvent> = callbackFlow {
        try {
            var connectedSocket: Socket? = null
            var connectError: IOException? = null

            // Ports to try: starting with the target port, then adjacent sync ports
            val portsToTry = buildList {
                add(port)
                for (p in SYNC_PORT_RANGE_START..(SYNC_PORT_RANGE_START + 5)) {
                    if (p != port) add(p)
                }
            }

            Log.d(TAG, "Client attempting connection to $host, ports to try: $portsToTry")

            portLoop@ for (targetPort in portsToTry) {
                try {
                    val s = Socket()
                    s.tcpNoDelay = true
                    s.connect(InetSocketAddress(host, targetPort), CONNECT_TIMEOUT_MS)
                    connectedSocket = s
                    Log.d(TAG, "Client successfully connected to $host:$targetPort")
                    break@portLoop
                } catch (e: IOException) {
                    connectError = e
                    Log.d(TAG, "Connection to $host:$targetPort failed: ${e.message}")
                }
            }

            if (connectedSocket == null) {
                throw connectError ?: IOException("Failed to connect to $host (ports tried: $portsToTry)")
            }

            socket = connectedSocket
            writer = connectedSocket.getOutputStream()
            trySend(TransportEvent.PeerConnected)

            val reader = BufferedReader(InputStreamReader(connectedSocket.getInputStream(), Charsets.UTF_8))
            while (!connectedSocket.isClosed) {
                val line = try {
                    reader.readLine()
                } catch (e: IOException) {
                    null
                } ?: break

                val message = SyncMessageCodec.decode(line)
                if (message != null) {
                    Log.d(TAG, "Client received message: ${message.javaClass.simpleName}")
                    trySend(TransportEvent.Message(message))
                } else {
                    Log.w(TAG, "Client dropped malformed message line")
                }
            }
            trySend(TransportEvent.Disconnected)
        } catch (e: IOException) {
            if (socket?.isClosed != true) {
                trySend(TransportEvent.Error(e))
            }
        }

        awaitClose { closeInternal() }
    }.flowOn(Dispatchers.IO)

    override suspend fun send(message: SyncMessage): Boolean = withContext(Dispatchers.IO) {
        val out = writer ?: run {
            Log.w(TAG, "Client send failed: no active socket writer")
            return@withContext false
        }
        try {
            val encoded = SyncMessageCodec.encode(message) + "\n"
            Log.d(TAG, "Client sending: ${message.javaClass.simpleName}")
            out.write(encoded.toByteArray(Charsets.UTF_8))
            out.flush()
            true
        } catch (e: IOException) {
            Log.w(TAG, "Client send error", e)
            false
        }
    }

    override fun close() = closeInternal()

    private fun closeInternal() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        writer = null
    }
}
