package com.vardansoft.komposeauth.domain

enum class Platform {
    DESKTOP,
    WEB,
    ANDROID,
    IOS
}
expect fun currentPlatform(): Platform