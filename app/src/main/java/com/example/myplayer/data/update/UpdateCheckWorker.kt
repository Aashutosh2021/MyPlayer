package com.example.myplayer.data.update

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * WorkManager worker that periodically checks for app updates in the background.
 * If a newer version is discovered and hasn't yet been notified to the user,
 * a notification is dispatched via UpdateNotifier.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateChecker: UpdateChecker,
    private val updateNotifier: UpdateNotifier
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UpdateCheckWorker"
        const val WORK_NAME = "com.example.myplayer.UPDATE_CHECK_WORK"
    }

    override suspend fun doWork(): Result {
        return try {
            val update = updateChecker.checkForUpdate()
            if (update != null && updateChecker.shouldNotifyFor(update)) {
                updateNotifier.show(update)
                updateChecker.markNotified(update)
            }
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Network error during background update check, will retry later: ${e.message}")
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Update check worker encountered unexpected error", e)
            Result.failure()
        }
    }
}
