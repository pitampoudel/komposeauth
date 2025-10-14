package com.vardansoft.core.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant


fun now(): Instant = Clock.System.now()
fun Instant.toSystemLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())
fun LocalDateTime.toInstant(): Instant = toInstant(TimeZone.currentSystemDefault())
fun Instant.toSystemLocalDate() = toSystemLocalDateTime().date
fun LocalDate.asDisplayDate(): String {
    val f = LocalDate.Format {
        year(); char('-'); monthNumber(); char('-'); day(padding = Padding.ZERO)
    }
    return f.format(this)
}

fun LocalDateTime.asDisplayDateTime(): String {
    val f = LocalDateTime.Format {
        date(LocalDate.Format {
            year(); char('-'); monthNumber(); char('-');
            day(padding = Padding.ZERO)
        })
        char(' ')
        hour(); char(':'); minute(); char(':'); second()
    }
    return f.format(this)
}

fun LocalTime.asDisplayTime(): String {
    val f = LocalTime.Format { hour(); char(':'); minute(); char(':'); second() }
    return f.format(this)
}
