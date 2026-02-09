package pitampoudel.core.presentation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Converts a past elapsed duration into a relative "time ago" string.
 * Example outputs: "just now", "1 min ago", "2 hrs ago", "5 days ago".
 * Set [short] to true for compact output like "1d", "3hrs", "5mins".
 */
fun Duration.toTimeAgoString(short: Boolean = false): String {
    val d = this.absoluteValue
    val nowText = if (short) "now" else "just now"
    if (d < 10.minutes) return nowText

    val days = d.inWholeDays
    if (days >= 1) return formatAgo(days, "day", short)

    val hours = d.inWholeHours
    if (hours >= 1) return formatAgo(hours, "hr", short)

    val mins = d.inWholeMinutes
    if (mins >= 1) return formatAgo(mins, "min", short)

    return nowText
}

private fun formatAgo(value: Long, unit: String, short: Boolean): String {
    if (short) {
        return when (unit) {
            "day" -> "${value}d"
            "hr" -> if (value == 1L) "${value}hr" else "${value}hrs"
            "min" -> if (value == 1L) "${value}min" else "${value}mins"
            else -> "$value$unit"
        }
    }

    val plural = when (unit) {
        "hr" -> if (value == 1L) "hr" else "hrs"
        else -> if (value == 1L) unit else "${unit}s"
    }
    return "$value $plural ago"
}
