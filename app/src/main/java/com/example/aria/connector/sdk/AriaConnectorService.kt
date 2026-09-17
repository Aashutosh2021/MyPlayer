package com.example.aria.connector.sdk

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log
import com.example.aria.connector.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Base Android Service that ecosystem apps (e.g. MyPlayer) extend to integrate with ARIA.
 */
abstract class AriaConnectorService : Service() {

    private val tag = "AriaConnectorService[${javaClass.simpleName}]"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val callbacks = RemoteCallbackList<IAriaCallback>()

    // ── Subclass contract ─────────────────────────────────────────────────────

    /** Return the human-readable name of your app. */
    abstract fun appName(): String

    /** Declare which capabilities your app currently supports. */
    abstract fun supportedCapabilities(): Set<Capability>

    /** Handle an [AppCommand] from ARIA. */
    abstract suspend fun handleCommand(command: AppCommand): AppResponse

    /** Return version info of your connector app. */
    open fun getVersionInfo(): ConnectorVersionInfo {
        return ConnectorVersionInfo(
            sdkVersion = 2,
            appVersionCode = 1,
            appVersionName = "2.0",
            supportedFeatures = supportedCapabilities().map { it.name }
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Push a typed event to ARIA. */
    protected fun notifyEvent(event: EcosystemEvent) {
        val n = callbacks.beginBroadcast()
        try {
            val container = EcosystemEventContainer(event)
            for (i in 0 until n) {
                try {
                    callbacks.getBroadcastItem(i).onEvent(container)
                } catch (_: Exception) {}
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    /** Call when your app's capability set changes at runtime. */
    protected fun notifyCapabilitiesChanged() {
        val caps = supportedCapabilities().toList()
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                try {
                    callbacks.getBroadcastItem(i).onCapabilitiesChanged(caps)
                } catch (_: Exception) {}
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    // ── Legacy Event Wrappers for Backward Compatibility ──────────────────────

    @Deprecated("Use notifyEvent(EcosystemEvent)")
    protected fun broadcastEvent(event: String, payload: String) {
        try {
            val ecoEvent = when (event) {
                "TRACK_CHANGED" -> {
                    val obj = org.json.JSONObject(payload)
                    EcosystemEvent.TrackChanged(obj.optString("track"), obj.optString("artist"))
                }
                "PLAYBACK_STOPPED" -> EcosystemEvent.PlaybackStopped
                "PLAYBACK_PAUSED" -> EcosystemEvent.PlaybackPaused
                "DOWNLOAD_COMPLETE" -> {
                    val obj = org.json.JSONObject(payload)
                    EcosystemEvent.DownloadComplete("", obj.optString("track"))
                }
                "SLEEP_TIMER_DONE" -> EcosystemEvent.SleepTimerDone
                else -> null
            }
            if (ecoEvent != null) {
                notifyEvent(ecoEvent)
            }
        } catch (_: Exception) {}
    }

    protected fun notifyTrackChanged(trackTitle: String, artist: String = "", albumArt: String = "") {
        notifyEvent(EcosystemEvent.TrackChanged(trackTitle, artist))
    }

    protected fun notifyPlaybackStopped() = notifyEvent(EcosystemEvent.PlaybackStopped)

    protected fun notifyPlaybackPaused() = notifyEvent(EcosystemEvent.PlaybackPaused)

    protected fun notifyDownloadComplete(trackTitle: String) {
        notifyEvent(EcosystemEvent.DownloadComplete("", trackTitle))
    }

    protected fun notifySleepTimerDone() = notifyEvent(EcosystemEvent.SleepTimerDone)

    // ── AIDL binder ───────────────────────────────────────────────────────────

    private val binderImpl = object : IAriaConnector.Stub() {

        override fun execute(container: AppCommandContainer): AppResponseContainer {
            var result: AppResponse = AppResponse.Error(AppResponse.Error.ErrorCode.UNKNOWN, "")
            val job = scope.launch {
                result = try {
                    handleCommand(container.command)
                } catch (e: Exception) {
                    Log.e(tag, "handleCommand threw: ${e.message}")
                    AppResponse.Error(AppResponse.Error.ErrorCode.COMMAND_FAILED, e.message ?: "Error")
                }
            }
            runBlocking { job.join() }
            return AppResponseContainer(result)
        }

        override fun getCapabilities(): List<Capability> {
            return this@AriaConnectorService.supportedCapabilities().toList()
        }

        override fun getVersionInfo(): ConnectorVersionInfo {
            return this@AriaConnectorService.getVersionInfo()
        }

        override fun getAppName(): String = this@AriaConnectorService.appName()

        override fun registerCallback(callback: IAriaCallback) {
            callbacks.register(callback)
            Log.d(tag, "ARIA callback registered")
        }

        override fun unregisterCallback(callback: IAriaCallback) {
            callbacks.unregister(callback)
        }

        override fun executeLegacy(commandJson: String): String {
            val command = AppCommand.fromJson(commandJson)
                ?: return AppResponse.Error(
                    AppResponse.Error.ErrorCode.UNKNOWN,
                    "Unrecognized command: $commandJson"
                ).toJson()
            var result: AppResponse = AppResponse.Error(AppResponse.Error.ErrorCode.UNKNOWN, "")
            val job = scope.launch {
                result = try {
                    handleCommand(command)
                } catch (e: Exception) {
                    Log.e(tag, "handleCommand threw: ${e.message}")
                    AppResponse.Error(AppResponse.Error.ErrorCode.COMMAND_FAILED, e.message ?: "Error")
                }
            }
            runBlocking { job.join() }
            return result.toJson()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i(tag, "ARIA bound to ${appName()}")
        return binderImpl
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i(tag, "ARIA unbound from ${appName()}")
        return super.onUnbind(intent)
    }
}
