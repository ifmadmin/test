package com.icscalendar.sync.parser

import com.icscalendar.sync.data.IcsEvent
import java.text.SimpleDateFormat
import java.util.*

class IcsParser {

    fun parse(icsContent: String): List<IcsEvent> {
        val events = mutableListOf<IcsEvent>()
        val lines = unfoldLines(icsContent.lines())

        var inEvent = false
        var currentEvent = mutableMapOf<String, String>()
        var exdates = mutableListOf<Long>()

        for (line in lines) {
            when {
                line.startsWith("BEGIN:VEVENT") -> {
                    inEvent = true
                    currentEvent = mutableMapOf()
                    exdates = mutableListOf()
                }
                line.startsWith("END:VEVENT") -> {
                    inEvent = false
                    val event = createEvent(currentEvent, exdates)
                    if (event != null) {
                        events.add(event)
                    }
                }
                inEvent -> {
                    val colonIndex = line.indexOf(':')
                    if (colonIndex > 0) {
                        val key = line.substring(0, colonIndex)
                        val value = line.substring(colonIndex + 1)

                        if (key.startsWith("EXDATE")) {
                            parseDateTime(value, key.contains("VALUE=DATE"))?.let {
                                exdates.add(it)
                            }
                        } else {
                            val baseKey = key.split(';').first()
                            currentEvent[baseKey] = value
                            // Store the full key for date parsing hints
                            if (baseKey == "DTSTART" || baseKey == "DTEND") {
                                currentEvent["${baseKey}_PARAMS"] = key
                            }
                        }
                    }
                }
            }
        }

        return events
    }

    private fun unfoldLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()

        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                current.append(line.substring(1))
            } else {
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                }
                current.clear()
                current.append(line.trim())
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }

    private fun createEvent(props: Map<String, String>, exdates: List<Long>): IcsEvent? {
        val uid = props["UID"] ?: return null
        val summary = props["SUMMARY"]?.unescapeIcs() ?: return null

        val dtStartParams = props["DTSTART_PARAMS"] ?: ""
        val dtEndParams = props["DTEND_PARAMS"] ?: ""
        val isAllDay = dtStartParams.contains("VALUE=DATE") && !dtStartParams.contains("VALUE=DATE-TIME")

        val dtStart = parseDateTime(props["DTSTART"], isAllDay) ?: return null
        val dtEnd = parseDateTime(props["DTEND"], isAllDay)
            ?: parseDuration(dtStart, props["DURATION"])
            ?: (dtStart + 3600000) // Default 1 hour

        return IcsEvent(
            uid = uid,
            summary = summary,
            description = props["DESCRIPTION"]?.unescapeIcs(),
            location = props["LOCATION"]?.unescapeIcs(),
            dtStart = dtStart,
            dtEnd = dtEnd,
            allDay = isAllDay,
            rrule = props["RRULE"],
            exdates = exdates
        )
    }

    private fun parseDateTime(value: String?, isDateOnly: Boolean): Long? {
        if (value == null) return null

        return try {
            val cleanValue = value.trim()

            when {
                isDateOnly || cleanValue.length == 8 -> {
                    // Date only: YYYYMMDD
                    val format = SimpleDateFormat("yyyyMMdd", Locale.US)
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.parse(cleanValue.take(8))?.time
                }
                cleanValue.endsWith("Z") -> {
                    // UTC time: YYYYMMDDTHHmmssZ
                    val format = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.parse(cleanValue)?.time
                }
                cleanValue.contains("T") -> {
                    // Local time: YYYYMMDDTHHmmss
                    val format = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
                    format.timeZone = TimeZone.getDefault()
                    format.parse(cleanValue)?.time
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDuration(start: Long, duration: String?): Long? {
        if (duration == null) return null

        return try {
            var millis = 0L
            var remaining = duration.removePrefix("P")
            var inTime = false

            val pattern = Regex("(\\d+)([WDHMS])")
            pattern.findAll(remaining).forEach { match ->
                val num = match.groupValues[1].toLong()
                when (match.groupValues[2]) {
                    "W" -> millis += num * 7 * 24 * 60 * 60 * 1000
                    "D" -> millis += num * 24 * 60 * 60 * 1000
                    "H" -> millis += num * 60 * 60 * 1000
                    "M" -> millis += num * 60 * 1000
                    "S" -> millis += num * 1000
                }
            }

            start + millis
        } catch (e: Exception) {
            null
        }
    }

    private fun String.unescapeIcs(): String {
        return this
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
