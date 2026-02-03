package com.icscalendar.sync.data

import java.util.UUID

data class CalendarConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icsUrl: String,
    val syncIntervalMinutes: Int = 60,
    val color: Int = -14575885, // Default blue
    val lastSync: Long = 0,
    val eventsCount: Int = 0,
    val localCalendarId: Long = -1
)
