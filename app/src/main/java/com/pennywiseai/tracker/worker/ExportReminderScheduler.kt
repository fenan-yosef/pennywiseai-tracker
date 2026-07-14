package com.pennywiseai.tracker.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels the weekly export reminder notification.
 *
 * The reminder fires once a week on the chosen day (default: Friday)
 * at 10:00 AM local time. Uses WorkManager's periodic work with
 * an initial delay computed to land on the next chosen day.
 */
@Singleton
class ExportReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ExportReminderScheduler"
        private const val WORK_NAME = "export_reminder_work"

        /** Default reminder time: 10:00 AM */
        private val REMINDER_TIME = LocalTime.of(10, 0)

        /**
         * Map our day storage (1=Monday..7=Sunday) to Java's DayOfWeek.
         */
        fun intToDayOfWeek(day: Int): DayOfWeek = when (day) {
            1 -> DayOfWeek.MONDAY
            2 -> DayOfWeek.TUESDAY
            3 -> DayOfWeek.WEDNESDAY
            4 -> DayOfWeek.THURSDAY
            5 -> DayOfWeek.FRIDAY
            6 -> DayOfWeek.SATURDAY
            7 -> DayOfWeek.SUNDAY
            else -> DayOfWeek.FRIDAY
        }
    }

    /**
     * Schedule or update the weekly export reminder.
     * If already scheduled, it will be updated with the new day.
     *
     * @param dayOfWeek 1=Monday .. 7=Sunday
     */
    fun schedule(dayOfWeek: Int) {
        val targetDay = intToDayOfWeek(dayOfWeek)
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val nextReminder = now.with(TemporalAdjusters.next(targetDay))
            .withHour(REMINDER_TIME.hour)
            .withMinute(REMINDER_TIME.minute)
            .withSecond(0)
            .withNano(0)

        // If today is the target day and the time hasn't passed yet, use today
        val todayReminder = if (now.dayOfWeek == targetDay && now.toLocalTime().isBefore(REMINDER_TIME)) {
            now.withHour(REMINDER_TIME.hour)
                .withMinute(REMINDER_TIME.minute)
                .withSecond(0)
                .withNano(0)
        } else null

        val reminderTime = todayReminder ?: nextReminder
        val initialDelay = reminderTime.toEpochSecond() - now.toEpochSecond()

        val workRequest = PeriodicWorkRequestBuilder<ExportReminderWorker>(
            7, TimeUnit.DAYS // Repeat every 7 days
        ).setInitialDelay(
            maxOf(initialDelay, 60), TimeUnit.SECONDS // At least 1 minute from now
        ).addTag(WORK_NAME).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }

    /**
     * Cancel the weekly export reminder.
     */
    fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(WORK_NAME)
    }
}
