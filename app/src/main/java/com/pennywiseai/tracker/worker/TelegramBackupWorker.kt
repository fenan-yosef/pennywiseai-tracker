package com.pennywiseai.tracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pennywiseai.tracker.data.backup.BackupExporter
import com.pennywiseai.tracker.data.backup.ExportResult
import com.pennywiseai.tracker.data.backup.TelegramUploader
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class TelegramBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupExporter: BackupExporter,
    private val telegramUploader: TelegramUploader
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "telegram_auto_backup_work"
        private const val TAG = "TelegramBackupWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val autoBackupEnabled = userPreferencesRepository.telegramAutoBackupEnabled.first()
            if (!autoBackupEnabled) {
                Log.d(TAG, "Automatic Telegram backup is disabled")
                return Result.success()
            }

            val botToken = userPreferencesRepository.telegramBotToken.first()
            val chatId = userPreferencesRepository.telegramChatId.first()

            if (botToken.isBlank() || chatId.isBlank()) {
                Log.w(TAG, "Telegram credentials incomplete for auto-backup")
                return Result.success()
            }

            // Export app backup
            val exportResult = backupExporter.exportBackup()
            if (exportResult !is ExportResult.Success) {
                Log.e(TAG, "Failed to export backup for Telegram auto-backup")
                return Result.retry()
            }

            val file = exportResult.file
            val timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val caption = "🤖 *PennyWise Automated Weekly Backup*\n📅 Date: $timestampStr\n📁 File: `${file.name}`"

            val uploadResult = telegramUploader.uploadBackupFile(
                botToken = botToken,
                chatId = chatId,
                backupFile = file,
                caption = caption
            )

            if (uploadResult.isSuccess) {
                Log.d(TAG, "Telegram automated weekly backup completed successfully!")
                userPreferencesRepository.setTelegramLastBackupTime(System.currentTimeMillis())
                Result.success()
            } else {
                Log.e(TAG, "Telegram upload failed", uploadResult.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TelegramBackupWorker", e)
            Result.retry()
        }
    }
}
