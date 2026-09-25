package com.example.myplayer.sync.transport

import com.example.myplayer.sync.model.SyncMessage
import kotlinx.coroutines.flow.Flow

interface SyncTransport {
    suspend fun send(message: SyncMessage)
    fun observeMessages(): Flow<SyncMessage>
    suspend fun disconnect()
    fun isConnected(): Boolean
}
