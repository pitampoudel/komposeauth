package com.vardansoft.authx.ui.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun LocalDate.asDisplayDate(): String {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return dateFormatter.format(this.toJavaLocalDate())
}

actual fun LocalDateTime.asDisplayDateTime(): String {
    val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
    return dateFormatter.withZone(ZoneId.systemDefault()).format(this.toJavaLocalDateTime())
}

actual fun LocalTime.asDisplayTime(): String {
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return timeFormatter.format(this.toJavaLocalTime())
}