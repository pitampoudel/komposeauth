package com.vardansoft.core.domain

import kotlin.time.Instant

@JsFun("() => Date.now()")
external fun getCurrentTime(): Double

actual fun now(): Instant = Instant.fromEpochMilliseconds(getCurrentTime().toLong())
