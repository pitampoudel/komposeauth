@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.vardansoft.core.domain

import android.os.Build
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import java.text.DateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date

actual fun LocalDate.asDisplayDate(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        dateFormatter.format(this.toJavaLocalDate())
    } else {
        val instant = this.atStartOfDayIn(TimeZone.currentSystemDefault())
        val date = Date(instant.toEpochMilliseconds())
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
    }
}

actual fun LocalDateTime.asDisplayDateTime(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
        dateFormatter.withZone(ZoneId.systemDefault()).format(this.toJavaLocalDateTime())
    } else {
        val instant = this.toInstant(TimeZone.currentSystemDefault())
        val date = Date(instant.toEpochMilliseconds())
        DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date)
    }
}

actual fun LocalTime.asDisplayTime(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        timeFormatter.format(this.toJavaLocalTime())
    } else {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, this.hour)
        calendar.set(Calendar.MINUTE, this.minute)
        calendar.set(Calendar.SECOND, this.second)
        val date = calendar.time
        DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
    }
}
