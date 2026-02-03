package com.icscalendar.sync.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CalendarRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCalendars(): List<CalendarConfig> {
        val json = prefs.getString(KEY_CALENDARS, null) ?: return emptyList()
        val type = object : TypeToken<List<CalendarConfig>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCalendar(calendar: CalendarConfig) {
        val calendars = getCalendars().toMutableList()
        val index = calendars.indexOfFirst { it.id == calendar.id }
        if (index >= 0) {
            calendars[index] = calendar
        } else {
            calendars.add(calendar)
        }
        saveCalendars(calendars)
    }

    fun deleteCalendar(calendarId: String) {
        val calendars = getCalendars().filterNot { it.id == calendarId }
        saveCalendars(calendars)
    }

    fun getCalendar(calendarId: String): CalendarConfig? {
        return getCalendars().find { it.id == calendarId }
    }

    fun updateLastSync(calendarId: String, eventsCount: Int) {
        val calendar = getCalendar(calendarId) ?: return
        saveCalendar(calendar.copy(
            lastSync = System.currentTimeMillis(),
            eventsCount = eventsCount
        ))
    }

    fun updateLocalCalendarId(calendarId: String, localCalendarId: Long) {
        val calendar = getCalendar(calendarId) ?: return
        saveCalendar(calendar.copy(localCalendarId = localCalendarId))
    }

    private fun saveCalendars(calendars: List<CalendarConfig>) {
        val json = gson.toJson(calendars)
        prefs.edit().putString(KEY_CALENDARS, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "ics_calendar_sync_prefs"
        private const val KEY_CALENDARS = "calendars"
    }
}
