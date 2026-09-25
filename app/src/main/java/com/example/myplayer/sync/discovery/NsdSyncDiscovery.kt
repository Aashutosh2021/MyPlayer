package com.example.myplayer.sync.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.myplayer.sync.model.DiscoveredSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdSyncDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SyncDiscovery {

    companion object {
        private const val TAG = "NsdSyncDiscovery"
        const val SERVICE_TYPE = "_myplayersync._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false
    private var isAdvertising = false

    private val sessionsMap = ConcurrentHashMap<String, DiscoveredSession>()
    private val _discoveredSessions = MutableStateFlow<List<DiscoveredSession>>(emptyList())
    override val discoveredSessions: StateFlow<List<DiscoveredSession>> = _discoveredSessions.asStateFlow()

    private val resolveMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun getLocalIpAddress(): String? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            if (network != null) {
                val linkProps = cm.getLinkProperties(network)
                val ip = linkProps?.linkAddresses?.firstOrNull {
                    it.address is java.net.Inet4Address && !it.address.isLoopbackAddress
                }?.address?.hostAddress
                if (!ip.isNullOrBlank()) return ip
            }

            // Fallback: check network interfaces (wlan, ap, eth)
            val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isUp && (intf.name.contains("wlan") || intf.name.contains("ap") || intf.name.contains("eth"))) {
                    for (addr in java.util.Collections.list(intf.inetAddresses)) {
                        if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
            for (intf in interfaces) {
                if (intf.isUp) {
                    for (addr in java.util.Collections.list(intf.inetAddresses)) {
                        if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IP: ${e.message}")
        }
        return null
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("MyPlayerSyncDiscoveryLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release multicast lock: ${e.message}")
        }
    }

    override fun startAdvertising(
        sessionId: String,
        sessionName: String,
        masterName: String,
        port: Int
    ) {
        stopAdvertising()
        acquireMulticastLock()

        val localIp = getLocalIpAddress()
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "MyPlayer-$sessionId"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("sessionId", sessionId)
            setAttribute("sessionName", sessionName)
            setAttribute("masterName", masterName)
            if (!localIp.isNullOrBlank()) {
                setAttribute("hostIp", localIp)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                isAdvertising = true
                Log.i(TAG, "NSD Service registered: ${NsdServiceInfo.serviceName} on port $port, ip=$localIp")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isAdvertising = false
                Log.e(TAG, "NSD Registration failed: errorCode=$errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                isAdvertising = false
                Log.i(TAG, "NSD Service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Unregistration failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call registerService: ${e.message}")
        }
    }

    override fun stopAdvertising() {
        if (isAdvertising && registrationListener != null) {
            try {
                nsdManager?.unregisterService(registrationListener)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering service: ${e.message}")
            }
        }
        registrationListener = null
        isAdvertising = false
        releaseMulticastLock()
    }

    override fun startDiscovery() {
        if (isDiscovering) return
        stopDiscovery()
        acquireMulticastLock()

        sessionsMap.clear()
        _discoveredSessions.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                isDiscovering = true
                Log.i(TAG, "NSD discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}, type=${service.serviceType}")
                if (service.serviceType.contains("myplayersync")) {
                    scope.launch {
                        resolveServiceWithLock(service)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                sessionsMap.remove(service.serviceName)
                _discoveredSessions.value = sessionsMap.values.toList()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
                Log.i(TAG, "NSD discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                Log.e(TAG, "NSD Start discovery failed: errorCode=$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "NSD Stop discovery failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call discoverServices: ${e.message}")
        }
    }

    private suspend fun resolveServiceWithLock(serviceInfo: NsdServiceInfo) {
        resolveMutex.withLock {
            val completer = CompletableDeferred<Unit>()
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: errorCode=$errorCode")
                    completer.complete(Unit)
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                    val directIpAttr = resolvedInfo.attributes["hostIp"]?.let { String(it, Charsets.UTF_8) }
                    val nsdHost = resolvedInfo.host?.hostAddress
                    val host = when {
                        !directIpAttr.isNullOrBlank() && !directIpAttr.startsWith("10.0.2.") -> directIpAttr
                        !nsdHost.isNullOrBlank() -> nsdHost
                        else -> directIpAttr
                    }
                    val port = resolvedInfo.port
                    if (host != null && port > 0) {
                        val sessionId = resolvedInfo.attributes["sessionId"]?.let { String(it, Charsets.UTF_8) }
                            ?: resolvedInfo.serviceName.removePrefix("MyPlayer-")
                        val sessionName = resolvedInfo.attributes["sessionName"]?.let { String(it, Charsets.UTF_8) }
                            ?: resolvedInfo.serviceName
                        val masterName = resolvedInfo.attributes["masterName"]?.let { String(it, Charsets.UTF_8) }
                            ?: "Master Device"

                        val session = DiscoveredSession(
                            sessionId = sessionId,
                            sessionName = sessionName,
                            masterName = masterName,
                            hostAddress = host,
                            port = port
                        )
                        sessionsMap[resolvedInfo.serviceName] = session
                        _discoveredSessions.value = sessionsMap.values.toList()
                        Log.i(TAG, "Resolved session: $sessionName at $host:$port (id=$sessionId, nsdHost=$nsdHost, hostIpAttr=$directIpAttr)")
                    }
                    completer.complete(Unit)
                }
            }

            try {
                nsdManager?.resolveService(serviceInfo, resolveListener)
                // Wait with a 3-second timeout for this resolve to finish before next resolve
                withTimeoutOrNull(3000L) {
                    completer.await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception during resolveService: ${e.message}")
            }
        }
    }

    override fun stopDiscovery() {
        if (isDiscovering && discoveryListener != null) {
            try {
                nsdManager?.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping discovery: ${e.message}")
            }
        }
        discoveryListener = null
        isDiscovering = false
        releaseMulticastLock()
    }
}
