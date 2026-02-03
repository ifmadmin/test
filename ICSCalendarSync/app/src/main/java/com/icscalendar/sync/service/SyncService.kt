package com.icscalendar.sync.service

import android.content.Context
import com.icscalendar.sync.data.CalendarConfig
import com.icscalendar.sync.data.CalendarRepository
import com.icscalendar.sync.parser.IcsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SyncService(private val context: Context) {

    private val repository = CalendarRepository(context)
    private val calendarManager = CalendarManager(context)
    private val parser = IcsParser()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun syncCalendar(config: CalendarConfig): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Fetch ICS content
            val icsContent = fetchIcsContent(config.icsUrl)
                ?: return@withContext Result.failure(Exception("Failed to fetch ICS file"))

            // Parse ICS content
            val events = parser.parse(icsContent)

            // Get or create local calendar
            val localCalendarId = calendarManager.getOrCreateCalendar(config)
            if (localCalendarId <= 0) {
                return@withContext Result.failure(Exception("Failed to create local calendar"))
            }

            // Update local calendar ID if changed
            if (config.localCalendarId != localCalendarId) {
                repository.updateLocalCalendarId(config.id, localCalendarId)
            }

            // Sync events to local calendar
            val syncedCount = calendarManager.syncEvents(localCalendarId, events)

            // Update last sync time
            repository.updateLastSync(config.id, syncedCount)

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchIcsContent(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ICSCalendarSync/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncAllCalendars(): Map<String, Result<Int>> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Result<Int>>()
        val calendars = repository.getCalendars()

        for (calendar in calendars) {
            results[calendar.id] = syncCalendar(calendar)
        }

        results
    }

    fun deleteCalendarData(config: CalendarConfig) {
        if (config.localCalendarId > 0) {
            calendarManager.deleteCalendar(config.localCalendarId)
        }
    }
}
