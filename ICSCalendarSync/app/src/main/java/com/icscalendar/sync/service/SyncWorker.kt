package com.icscalendar.sync.service

import android.content.Context
import androidx.work.*
import com.icscalendar.sync.data.CalendarRepository
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val calendarId = inputData.getString(KEY_CALENDAR_ID)
        val syncService = SyncService(applicationContext)
        val repository = CalendarRepository(applicationContext)

        return try {
            if (calendarId != null) {
                // Sync specific calendar
                val config = repository.getCalendar(calendarId)
                if (config != null) {
                    val result = syncService.syncCalendar(config)
                    if (result.isSuccess) Result.success() else Result.retry()
                } else {
                    Result.failure()
                }
            } else {
                // Sync all calendars
                syncService.syncAllCalendars()
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_CALENDAR_ID = "calendar_id"
        private const val TAG_PREFIX = "sync_calendar_"

        fun schedulePeriodicSync(context: Context, calendarId: String, intervalMinutes: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_CALENDAR_ID, calendarId)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes.toLong(),
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG_PREFIX + calendarId)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    TAG_PREFIX + calendarId,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }

        fun cancelSync(context: Context, calendarId: String) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(TAG_PREFIX + calendarId)
        }

        fun syncNow(context: Context, calendarId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_CALENDAR_ID, calendarId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context)
                .enqueue(workRequest)
        }

        fun scheduleAllSyncs(context: Context) {
            val repository = CalendarRepository(context)
            for (calendar in repository.getCalendars()) {
                schedulePeriodicSync(context, calendar.id, calendar.syncIntervalMinutes)
            }
        }
    }
}
