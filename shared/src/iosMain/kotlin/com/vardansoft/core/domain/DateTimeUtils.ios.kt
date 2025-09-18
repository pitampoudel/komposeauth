package com.vardansoft.core.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number

actual fun LocalDate.asDisplayDate(): String {
    return "$day-${month.number}-${year}"
}

actual fun LocalDateTime.asDisplayDateTime(): String {
    return "$day-${month.number}-${year} ${hour}:${minute}:${second}"
}

actual fun LocalTime.asDisplayTime(): String {
    return "${hour}:${minute}:${second}"
}
