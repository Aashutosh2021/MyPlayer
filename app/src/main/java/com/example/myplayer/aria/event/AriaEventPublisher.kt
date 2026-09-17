package com.example.myplayer.aria.event

import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AriaEventPublisher @Inject constructor() {
    companion object {
        private const val TAG = "AriaEventPublisher"

        // Event identifiers passed in Message.what
        const val EVENT_SONG_STARTED           = 100
        const val EVENT_SONG_ENDED             = 101
        const val EVENT_SONG_PAUSED            = 102
        const val EVENT_SONG_CHANGED           = 103
        const val EVENT_DOWNLOAD_STARTED       = 104
        const val EVENT_DOWNLOAD_COMPLETED     = 105
        const val EVENT_DOWNLOAD_FAILED        = 106
        const val EVENT_QUEUE_UPDATED          = 107
        const val EVENT_PLAYLIST_UPDATED        = 108
        const val EVENT_EQUALIZER_CHANGED      = 109
        const val EVENT_HISTORY_UPDATED        = 110
        const val EVENT_RECOMMENDATION_UPDATED = 111
    }

    // Map of listener Messenger to its associated DeathRecipient to prevent resource leaks
    private val listeners = ConcurrentHashMap<IBinder, ListenerRegistration>()

    private class ListenerRegistration(
        val messenger: Messenger,
        val deathRecipient: IBinder.DeathRecipient
    )

    fun registerListener(messenger: Messenger) {
        val binder = messenger.binder
        if (listeners.containsKey(binder)) return

        val deathRecipient = IBinder.DeathRecipient {
            Log.w(TAG, "Client binder died, automatically unregistering listener: $messenger")
            unregisterListener(messenger)
        }

        try {
            binder.linkToDeath(deathRecipient, 0)
            listeners[binder] = ListenerRegistration(messenger, deathRecipient)
            Log.i(TAG, "Registered event listener: $messenger (Total active: ${listeners.size})")
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to link to binder death for registration", e)
        }
    }

    fun unregisterListener(messenger: Messenger) {
        val binder = messenger.binder
        val registration = listeners.remove(binder)
        if (registration != null) {
            try {
                binder.unlinkToDeath(registration.deathRecipient, 0)
            } catch (e: Exception) {
                // Ignore if binder is already dead
            }
            Log.i(TAG, "Unregistered event listener: $messenger (Total active: ${listeners.size})")
        }
    }

    fun publishEvent(eventCode: Int, data: Bundle = Bundle()) {
        if (listeners.isEmpty()) return
        Log.d(TAG, "Publishing event code $eventCode to ${listeners.size} listeners")

        val deadBinders = mutableListOf<IBinder>()

        for ((binder, registration) in listeners) {
            try {
                val message = Message.obtain().apply {
                    what = eventCode
                    setData(data)
                }
                registration.messenger.send(message)
            } catch (e: RemoteException) {
                Log.w(TAG, "RemoteException sending event to listener, marking for removal")
                deadBinders.add(binder)
            }
        }

        // Clean up any dead listeners
        for (binder in deadBinders) {
            val registration = listeners.remove(binder)
            if (registration != null) {
                try {
                    binder.unlinkToDeath(registration.deathRecipient, 0)
                } catch (e: Exception) {}
            }
        }
    }
}
