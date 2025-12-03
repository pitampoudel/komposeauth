package pitampoudel.komposeauth.core.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

fun HttpServletRequest.logout(response: HttpServletResponse) {
    logout()
    val cookie = ResponseCookie.from("ACCESS_TOKEN", "")
        .httpOnly(true)
        .secure(isSecure)
        .path("/")
        .sameSite("None")
        .maxAge(0)
        .build()
    response.setHeader("Set-Cookie", cookie.toString())
}

fun HttpServletRequest.login(accessToken: String, response: HttpServletResponse) {
    val cookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
        .httpOnly(true)
        .secure(isSecure)
        .path("/")
        .sameSite("None")
        .maxAge((1.days - 1.minutes).toJavaDuration())
        .build()
    response.addHeader("Set-Cookie", cookie.toString())
}