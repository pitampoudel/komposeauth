package pitampoudel.core.presentation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Converts a past elapsed duration into a relative "time ago" string.
 * Example outputs: "just now", "1 min", "2 hrs", "5 days".
 * Set [short] to true for compact output like "1d", "3hrs", "5mins".
 */
fun Duration.asString(short: Boolean = false): String {
    val d = this.absoluteValue
    val nowText = if (short) "now" else "just now"
    if (d < 10.minutes) return nowText

    val days = d.inWholeDays
    if (days >= 1) return asString(days, "day", short)

    val hours = d.inWholeHours
    if (hours >= 1) return asString(hours, "hr", short)

    val mins = d.inWholeMinutes
    if (mins >= 1) return asString(mins, "min", short)

    return nowText
}

private fun asString(value: Long, unit: String, short: Boolean): String {
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
    return "$value $plural"
}
