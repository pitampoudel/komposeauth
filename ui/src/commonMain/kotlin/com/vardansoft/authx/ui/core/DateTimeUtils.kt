package com.vardansoft.authx.ui.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalTime::class)
fun Instant.toSystemLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())
@OptIn(ExperimentalTime::class)
fun Instant.toSystemLocalDate() = toSystemLocalDateTime().date

expect fun LocalDate.asDisplayDate(): String
expect fun LocalDateTime.asDisplayDateTime(): String
expect fun LocalTime.asDisplayTime(): String