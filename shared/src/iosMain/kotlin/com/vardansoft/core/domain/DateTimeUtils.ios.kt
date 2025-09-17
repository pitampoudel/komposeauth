package com.vardansoft.core.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

actual fun LocalDate.asDisplayDate(): String {
    return "${dayOfMonth}-${monthNumber}-${year}"
}

actual fun LocalDateTime.asDisplayDateTime(): String {
    return "${dayOfMonth}-${monthNumber}-${year} ${hour}:${minute}:${second}"
}

actual fun LocalTime.asDisplayTime(): String {
    return "${hour}:${minute}:${second}"
}
