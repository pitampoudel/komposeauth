package com.vardansoft.core.domain

import kotlin.time.Clock
import kotlin.time.Instant

actual fun now(): Instant = Clock.System.now()