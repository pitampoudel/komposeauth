package pitampoudel.komposeauth.core.domain

/**
 * Represents how the server should deliver authentication on login.
 * - TOKEN: return OAuth2-style token payload in the response body.
 * - SESSION: create an authenticated server session.
 * - COOKIE: set HttpOnly access token cookie.
 */
enum class ResponseType {
    TOKEN,
    COOKIE,
    SESSION
}
