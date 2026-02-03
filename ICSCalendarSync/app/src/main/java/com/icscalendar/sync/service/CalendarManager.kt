package com.icscalendar.sync.service

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.icscalendar.sync.data.CalendarConfig
import com.icscalendar.sync.data.IcsEvent
import java.util.TimeZone

class CalendarManager(private val context: Context) {

    companion object {
        // Use our own account type for sync adapter
        private const val ACCOUNT_NAME = AccountAuthenticator.ACCOUNT_NAME
        private const val ACCOUNT_TYPE = AccountAuthenticator.ACCOUNT_TYPE
    }

    init {
        // Ensure account exists
        AccountAuthenticator.createAccount(context)
    }

    fun getOrCreateCalendar(config: CalendarConfig): Long {
        // Ensure account exists
        AccountAuthenticator.createAccount(context)

        // Check if calendar already exists
        if (config.localCalendarId > 0) {
            if (calendarExists(config.localCalendarId)) {
                updateCalendarColor(config.localCalendarId, config.color)
                return config.localCalendarId
            }
        }

        // Try to find existing calendar by name
        val existingId = findCalendarByName(config.name)
        if (existingId > 0) {
            updateCalendarColor(existingId, config.color)
            return existingId
        }

        // Create new calendar
        return createCalendar(config)
    }

    private fun calendarExists(calendarId: Long): Boolean {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    private fun findCalendarByName(name: String): Long {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(name, ACCOUNT_NAME, ACCOUNT_TYPE)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return -1
    }

    private fun createCalendar(config: CalendarConfig): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, config.name)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, config.name)
            put(CalendarContract.Calendars.CALENDAR_COLOR, config.color)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }

        val uri = asSyncAdapter(CalendarContract.Calendars.CONTENT_URI)
        val resultUri = context.contentResolver.insert(uri, values)
        return resultUri?.lastPathSegment?.toLongOrNull() ?: -1
    }

    private fun updateCalendarColor(calendarId: Long, color: Int) {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.CALENDAR_COLOR, color)
        }
        val uri = ContentUris.withAppendedId(
            asSyncAdapter(CalendarContract.Calendars.CONTENT_URI),
            calendarId
        )
        context.contentResolver.update(uri, values, null, null)
    }

    fun deleteCalendar(calendarId: Long) {
        if (calendarId <= 0) return

        val uri = ContentUris.withAppendedId(
            asSyncAdapter(CalendarContract.Calendars.CONTENT_URI),
            calendarId
        )
        context.contentResolver.delete(uri, null, null)
    }

    fun syncEvents(calendarId: Long, events: List<IcsEvent>): Int {
        if (calendarId <= 0) return 0

        // Delete all existing events for this calendar
        deleteAllEvents(calendarId)

        // Insert new events
        var count = 0
        for (event in events) {
            if (insertEvent(calendarId, event)) {
                count++
            }
        }

        return count
    }

    private fun deleteAllEvents(calendarId: Long) {
        val uri = asSyncAdapter(CalendarContract.Events.CONTENT_URI)
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())
        context.contentResolver.delete(uri, selection, selectionArgs)
    }

    private fun insertEvent(calendarId: Long, event: IcsEvent): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.summary)
            put(CalendarContract.Events.DESCRIPTION, event.description ?: "")
            put(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
            put(CalendarContract.Events.DTSTART, event.dtStart)
            put(CalendarContract.Events.DTEND, event.dtEnd)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events._SYNC_ID, event.uid)

            if (event.allDay) {
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            }

            if (event.rrule != null) {
                put(CalendarContract.Events.RRULE, event.rrule)
            }
        }

        val uri = asSyncAdapter(CalendarContract.Events.CONTENT_URI)
        val resultUri = context.contentResolver.insert(uri, values)

        // Handle EXDATE for recurring events
        if (resultUri != null && event.rrule != null && event.exdates.isNotEmpty()) {
            val eventId = resultUri.lastPathSegment?.toLongOrNull()
            if (eventId != null) {
                insertExdates(eventId, event.exdates)
            }
        }

        return resultUri != null
    }

    private fun insertExdates(eventId: Long, exdates: List<Long>) {
        for (exdate in exdates) {
            val values = ContentValues().apply {
                put(CalendarContract.Events.ORIGINAL_ID, eventId)
                put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, exdate)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED)
            }
            // Exceptions are more complex to handle properly, skipping for simplicity
        }
    }

    private fun asSyncAdapter(uri: Uri): Uri {
        return uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()
    }

    fun getEventsCount(calendarId: Long): Int {
        if (calendarId <= 0) return 0

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            return cursor.count
        }
        return 0
    }
}
