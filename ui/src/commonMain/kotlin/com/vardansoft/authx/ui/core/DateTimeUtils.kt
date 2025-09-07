package com.vardansoft.authx.ui.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun Instant.toSystemLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())
fun Instant.toSystemLocalDate() = toSystemLocalDateTime().date

expect fun LocalDate.asDisplayDate(): String
expect fun LocalDateTime.asDisplayDateTime(): String
expect fun LocalTime.asDisplayTime(): String