package com.pennywiseai.tracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pennywiseai.tracker.MainActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ExportReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "export_reminder_work"
        private const val CHANNEL_ID = "export_reminder_channel"
        private const val CHANNEL_NAME = "Export Reminders"
        private const val NOTIFICATION_ID = 9001
    }

    override suspend fun doWork(): Result {
        val enabled = userPreferencesRepository.exportReminderEnabled.first()
        if (!enabled) return Result.success()

        showReminderNotification()
        return Result.success()
    }

    private fun showReminderNotification() {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly reminders to export your PennyWise data backup"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_settings", true) // Signal to navigate to settings
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Day names for the notification text
        val dayNames = mapOf(
            1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
            4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday"
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📊 Time to Backup!")
            .setContentText("Your weekly PennyWise data backup is ready. Export now to keep your financial data safe.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Don't lose your transaction history! Export your PennyWise data backup to a safe place.\n\n" +
                             "• All transactions & categories\n" +
                             "• Account balances & cards\n" +
                             "• Subscriptions & settings\n\n" +
                             "Go to Settings → Export Data to create your backup.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Export Now",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
