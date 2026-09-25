package com.example.myplayer.sync.discovery

import com.example.myplayer.sync.model.DiscoveredSession
import kotlinx.coroutines.flow.StateFlow

interface SyncDiscovery {
    val discoveredSessions: StateFlow<List<DiscoveredSession>>
    fun startAdvertising(sessionId: String, sessionName: String, masterName: String, port: Int)
    fun stopAdvertising()
    fun startDiscovery()
    fun stopDiscovery()
    fun isWifiConnected(): Boolean
}
