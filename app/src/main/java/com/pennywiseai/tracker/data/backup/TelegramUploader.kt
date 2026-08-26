package com.pennywiseai.tracker.data.backup

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramUploader @Inject constructor() {

    companion object {
        private const val TAG = "TelegramUploader"
    }

    suspend fun uploadBackupFile(
        botToken: String,
        chatId: String,
        backupFile: File,
        caption: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (botToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token cannot be blank"))
        }
        if (chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Telegram Chat ID / Username cannot be blank"))
        }
        if (!backupFile.exists() || backupFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Backup file is empty or does not exist"))
        }

        // Telegram limit for bot API file upload is 50MB
        val maxSizeBytes = 50 * 1024 * 1024
        if (backupFile.length() > maxSizeBytes) {
            return@withContext Result.failure(IllegalStateException("Backup file exceeds Telegram 50MB limit"))
        }

        try {
            val boundary = "---PennyWiseBoundary" + System.currentTimeMillis()
            val LINE_FEED = "\r\n"
            val url = URL("https://api.telegram.org/bot$botToken/sendDocument")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 30000
                readTimeout = 60000
            }

            DataOutputStream(connection.outputStream).use { output ->
                // Write chat_id parameter
                output.writeBytes("--$boundary$LINE_FEED")
                output.writeBytes("Content-Disposition: form-data; name=\"chat_id\"$LINE_FEED")
                output.writeBytes("Content-Type: text/plain; charset=UTF-8$LINE_FEED$LINE_FEED")
                output.write(chatId.toByteArray(Charsets.UTF_8))
                output.writeBytes(LINE_FEED)

                // Write caption parameter
                output.writeBytes("--$boundary$LINE_FEED")
                output.writeBytes("Content-Disposition: form-data; name=\"caption\"$LINE_FEED")
                output.writeBytes("Content-Type: text/plain; charset=UTF-8$LINE_FEED$LINE_FEED")
                output.write(caption.toByteArray(Charsets.UTF_8))
                output.writeBytes(LINE_FEED)

                // Write document file
                output.writeBytes("--$boundary$LINE_FEED")
                output.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"${backupFile.name}\"$LINE_FEED")
                output.writeBytes("Content-Type: application/octet-stream$LINE_FEED$LINE_FEED")

                FileInputStream(backupFile).use { input ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
                output.writeBytes(LINE_FEED)
                output.writeBytes("--$boundary--$LINE_FEED")
                output.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Successfully uploaded backup to Telegram: $responseMessage")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed Telegram upload HTTP $responseCode: $responseMessage")
                Result.failure(Exception("Telegram API Error ($responseCode): $responseMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading backup file to Telegram", e)
            Result.failure(e)
        }
    }
}
