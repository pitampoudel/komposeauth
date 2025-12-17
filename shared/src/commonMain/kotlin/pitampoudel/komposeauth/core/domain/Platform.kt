package pitampoudel.komposeauth.core.domain

enum class Platform {
    DESKTOP,
    WEB,
    ANDROID,
    IOS
}
expect fun currentPlatform(): Platform