package pitampoudel.core.presentation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Converts a past elapsed duration into a relative "time ago" string.
 * Example outputs: "just now", "1 min ago", "2 hrs ago", "5 days ago"
 */
fun Duration.toTimeAgoString(): String {
    val d = this.absoluteValue // in case someone passes a negative duration
    if (d < 10.minutes) return "just now"

    val days = d.inWholeDays
    if (days >= 1) return formatAgo(days, "day")

    val hours = d.inWholeHours
    if (hours >= 1) return formatAgo(hours, "hr")

    val mins = d.inWholeMinutes
    if (mins >= 1) return formatAgo(mins, "min")

    return "just now"
}

private fun formatAgo(value: Long, unit: String): String {
    val plural = when (unit) {
        "hr" -> if (value == 1L) "hr" else "hrs"
        else -> if (value == 1L) unit else "${unit}s"
    }
    return "$value $plural ago"
}
