package com.icscalendar.sync.data

data class IcsEvent(
    val uid: String,
    val summary: String,
    val description: String?,
    val location: String?,
    val dtStart: Long,
    val dtEnd: Long,
    val allDay: Boolean = false,
    val rrule: String? = null,
    val exdates: List<Long> = emptyList()
)
