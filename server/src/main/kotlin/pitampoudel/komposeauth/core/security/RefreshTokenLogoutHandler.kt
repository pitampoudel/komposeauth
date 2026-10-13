package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.authorization.MongoOAuth2AuthorizationService
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.data.LogoutRequest

/**
 * Revokes the refresh token a sign-out sends ([LogoutRequest]), so a copy of it taken off the device
 * stops working too. Holding the token is the proof: a sign-out without one still ends the session
 * and clears the cookie, as it always has.
 */
@Component
class RefreshTokenLogoutHandler(
    private val oneTimeTokenService: OneTimeTokenService,
    private val authorizationService: MongoOAuth2AuthorizationService,
    private val json: Json
) : LogoutHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        val refreshToken = refreshTokenOf(request) ?: return
        try {
            if (oneTimeTokenService.revokeRefreshToken(refreshToken)) return
            authorizationService.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN)
                ?.let(authorizationService::remove)
        } catch (e: Exception) {
            // The device has forgotten the token either way; the session still ends below.
            log.error("Could not revoke the refresh token sent with a sign-out", e)
        }
    }

    private fun refreshTokenOf(request: HttpServletRequest): String? {
        if (request.contentType?.startsWith("application/json", ignoreCase = true) != true) return null
        val body = request.inputStream.readNBytes(MAX_BODY_BYTES).toString(Charsets.UTF_8)
        return try {
            json.decodeFromString<LogoutRequest>(body).refreshToken?.takeIf { it.isNotBlank() }
        } catch (_: IllegalArgumentException) {
            // Including SerializationException: not a body of ours.
            null
        }
    }

    private companion object {
        const val MAX_BODY_BYTES = 4096
    }
}
