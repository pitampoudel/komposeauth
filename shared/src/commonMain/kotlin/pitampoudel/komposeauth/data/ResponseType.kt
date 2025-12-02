package pitampoudel.komposeauth.data

/**
 * Represents how the server should deliver authentication on login.
 * - TOKEN: return OAuth2-style token payload in the response body.
 * - COOKIE: set HttpOnly access token cookie for web apps.
 * - SESSION: store Spring Security context in HTTP session.
 */
enum class ResponseType {
    TOKEN,
    COOKIE,
    SESSION
}
