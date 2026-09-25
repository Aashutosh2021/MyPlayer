package com.example.myplayer.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

const val SYNC_SERVICE_TYPE = "_myplayersync._tcp."
private const val TAG = "SyncDiscovery"

data class DiscoveredHost(
    val serviceName: String,
    val host: InetAddress,
    val port: Int
)

/**
 * Wraps NsdManager for MyPlayer Sync Play.
 *
 * Every method here guards against the two most common real-world NsdManager crashes:
 * 1. Calling unregister/stop when nothing is registered ("listener not registered")
 *    -> fixed by nulling out the listener reference once used, and no-oping if it's
 *       already null.
 * 2. Reusing one ResolveListener instance across multiple resolveService() calls
 *    -> fixed by building a fresh listener inline for every single resolve.
 *
 * Uses MulticastLock so that Samsung and other Android devices do not drop mDNS packets.
 */
@Singleton
class SyncDiscovery @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /** Starts advertising this device as a Sync Play room. Emits the (possibly
     *  OS-renamed, on name collision) registered service name once live. */
    fun advertise(serviceNamePrefix: String, port: Int): Flow<String> = callbackFlow {
        acquireMulticastLock()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = serviceNamePrefix
            serviceType = SYNC_SERVICE_TYPE
            setPort(port)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Advertising as: ${info.serviceName} on port $port")
                trySend(info.serviceName)
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Advertise failed: errorCode=$errorCode")
                close(IllegalStateException("NSD registration failed: $errorCode"))
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "Advertising stopped")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Unregister failed: errorCode=$errorCode")
            }
        }
        registrationListener = listener

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "registerService threw", e)
            close(e)
        }

        awaitClose { stopAdvertising() }
    }

    fun stopAdvertising() {
        val listener = registrationListener ?: return
        registrationListener = null
        try {
            nsdManager.unregisterService(listener)
        } catch (e: Exception) {
            Log.w(TAG, "unregisterService threw (already stopped?)", e)
        }
        releaseMulticastLock()
    }

    /** Starts discovering nearby Sync Play rooms. Emits each resolved host as found. */
    fun discover(): Flow<DiscoveredHost> = callbackFlow {
        acquireMulticastLock()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started for $serviceType")
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                if (!info.serviceType.contains("_myplayersync")) return
                Log.d(TAG, "Found service: ${info.serviceName}")
                resolveServiceSafely(info) { resolved -> trySend(resolved) }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                Log.d(TAG, "Lost service: ${info.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: errorCode=$errorCode")
                close(IllegalStateException("NSD discovery failed: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Discovery stop failed: errorCode=$errorCode")
            }
        }
        discoveryListener = listener

        try {
            nsdManager.discoverServices(SYNC_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "discoverServices threw", e)
            close(e)
        }

        awaitClose { stopDiscovery() }
    }

    fun stopDiscovery() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (e: Exception) {
            Log.w(TAG, "stopServiceDiscovery threw (already stopped?)", e)
        }
        releaseMulticastLock()
    }

    private fun resolveServiceSafely(info: NsdServiceInfo, onResolved: (DiscoveredHost) -> Unit) {
        // A fresh listener EVERY call — NsdManager throws "listener already in use"
        // if you try to reuse one across multiple resolveService() invocations.
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${info.serviceName}: errorCode=$errorCode")
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val address = info.host ?: return
                Log.d(TAG, "Resolved ${info.serviceName} -> ${address.hostAddress}:${info.port}")
                onResolved(DiscoveredHost(info.serviceName, address, info.port))
            }
        }
        try {
            nsdManager.resolveService(info, resolveListener)
        } catch (e: Exception) {
            Log.w(TAG, "resolveService threw", e)
        }
    }

    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        try {
            multicastLock = wifiManager.createMulticastLock("myplayer_sync_mdns").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire multicast lock (missing CHANGE_WIFI_MULTICAST_STATE?)", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not release multicast lock", e)
        }
        multicastLock = null
    }
}
