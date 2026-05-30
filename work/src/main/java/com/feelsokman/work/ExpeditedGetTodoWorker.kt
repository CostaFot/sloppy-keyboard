package com.feelsokman.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.feelsokman.common.coroutine.DispatcherProvider
import com.feelsokman.logging.logDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@HiltWorker
class ExpeditedGetTodoWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dispatcherProvider: DispatcherProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(dispatcherProvider.io) {
        logDebug { "Starting work" }
        val rr = WorkManager.getInstance(appContext).createCancelPendingIntent(id)
        delay(5000) // simulate a long running worker
        Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.todoForegroundInfo()

    companion object {

        const val TAG = "ExpeditedGetTodoWorker"
        fun getWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<ExpeditedGetTodoWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()
        }
    }
}

private const val NotificationId = 0
private const val NotificationChannelID = "NotificationChannel"


/**
 * Foreground information for sync on lower API levels when sync workers are being
 * run with a foreground service
 */
private fun Context.todoForegroundInfo() = ForegroundInfo(
    NotificationId,
    getTodoWorkNotification()
)

/**
 * Notification displayed on lower API levels when sync workers are being
 * run with a foreground service
 */
private fun Context.getTodoWorkNotification(): Notification {
    val channel = NotificationChannel(
        NotificationChannelID,
        "todo",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Description for this notification channel"
    }
    // Register the channel with the system, could do this on app onCreate too
    val notificationManager: NotificationManager? = this.getSystemService()

    notificationManager?.createNotificationChannel(channel)

    return NotificationCompat.Builder(
        this,
        NotificationChannelID
    ).apply {
        setSmallIcon(com.feelsokman.design.R.drawable.ic_launcher_foreground)
        setContentTitle("This is a content title")
        setContentText("hello")
        priority = NotificationCompat.PRIORITY_DEFAULT
    }.build()

}
