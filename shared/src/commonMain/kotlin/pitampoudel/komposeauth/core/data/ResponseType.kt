package pitampoudel.komposeauth.core.data

/**
 * Represents how the server should deliver authentication on login.
 * - TOKEN: return OAuth2-style token payload in the response body.
 * - COOKIE: set HttpOnly access token cookie for web apps.
 */
enum class ResponseType {
    TOKEN,
    COOKIE
}
