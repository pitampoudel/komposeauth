package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.Cookie
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.security.csrf.CsrfProtectionPolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rule that decides who must send a CSRF token.
 *
 * Both directions matter and they fail differently. Demanding a token where a cross-site form could
 * never reach is not a hole but an outage — a 403 the caller cannot diagnose, which is what the
 * whole JSON API was answering. Not demanding one where a form *can* reach is the actual hole.
 */
class CsrfProtectionPolicyTest {

    private val policy = CsrfProtectionPolicy.matcher()

    private fun request(
        method: String,
        path: String,
        contentType: String? = MediaType.APPLICATION_JSON_VALUE,
        authorization: String? = null,
        cookies: List<Cookie> = emptyList()
    ) = MockHttpServletRequest(method, path).apply {
        contentType?.let { this.contentType = it }
        authorization?.let { addHeader("Authorization", it) }
        if (cookies.isNotEmpty()) setCookies(*cookies.toTypedArray())
    }

    private fun requiresToken(
        method: String,
        path: String,
        contentType: String? = MediaType.APPLICATION_JSON_VALUE,
        authorization: String? = null,
        cookies: List<Cookie> = emptyList()
    ) = policy.matches(request(method, path, contentType, authorization, cookies))

    private val cookieAuthenticated = listOf(Cookie(ACCESS_TOKEN_COOKIE_NAME, "any-jwt"))

    // --- The JSON API: reachable only through a CORS preflight, so a token buys nothing ---

    @Test
    fun `the whole cookie-authenticated JSON API is reachable without a token`() {
        // Every write this server's own browser client makes. It authenticates with the
        // access-token cookie and never fetches a CSRF token, so each of these was a flat 403.
        val jsonWrites = listOf(
            "/${ApiEndpoints.SEND_OTP}",
            "/${ApiEndpoints.VERIFY_OTP}",
            "/${ApiEndpoints.UPDATE_PROFILE}",
            "/${ApiEndpoints.KYC_PERSONAL_INFO}",
            "/${ApiEndpoints.KYC_DOCUMENTS}",
            "/${ApiEndpoints.KYC_ADDRESS}",
            "/${ApiEndpoints.ORGANIZATIONS}",
            "/${ApiEndpoints.LOGOUT}",
            "/webauthn/register",
            "/webauthn/register/options"
        )

        jsonWrites.forEach { path ->
            assertFalse(
                requiresToken("POST", path, cookies = cookieAuthenticated),
                "$path demanded a CSRF token a JSON client has no way to know it needs"
            )
        }
    }

    @Test
    fun `a JSON delete is reachable too`() {
        assertFalse(
            requiresToken("DELETE", "/${ApiEndpoints.ORGANIZATIONS}", cookies = cookieAuthenticated)
        )
    }

    // --- What a cross-site form can actually reach, and so still needs a token ---

    @Test
    fun `saving configuration still needs one`() {
        // The one @ModelAttribute endpoint in the server, carrying every secret it holds.
        assertTrue(
            requiresToken(
                "POST",
                "/admin/config",
                contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                cookies = cookieAuthenticated
            )
        )
    }

    @Test
    fun `the browser sign-in form still needs one`() {
        assertTrue(
            requiresToken(
                "POST",
                "/session-login",
                contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
            )
        )
    }

    @Test
    fun `a bodiless write still needs one`() {
        // The console's role grant and revoke send no content type at all, and take their token
        // from the page they were served on.
        assertTrue(
            requiresToken(
                "POST",
                "/${ApiEndpoints.USERS}/507f1f77bcf86cd799439011/${ApiEndpoints.ROLES}/ADMIN",
                contentType = null,
                cookies = cookieAuthenticated
            )
        )
    }

    @Test
    fun `the other two form enctypes need one as well`() {
        listOf(MediaType.TEXT_PLAIN_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE).forEach { type ->
            assertTrue(
                requiresToken(
                    "POST",
                    "/${ApiEndpoints.UPDATE_PROFILE}",
                    contentType = type,
                    cookies = cookieAuthenticated
                ),
                "$type is submittable by a cross-site form and must still require a token"
            )
        }
    }

    @Test
    fun `a form content type with parameters is still a form`() {
        assertTrue(
            requiresToken(
                "POST",
                "/${ApiEndpoints.UPDATE_PROFILE}",
                contentType = "application/x-www-form-urlencoded; charset=UTF-8",
                cookies = cookieAuthenticated
            )
        )
    }

    @Test
    fun `an unreadable content type is treated as a form`() {
        // When we cannot tell what produced the request, the careful answer is the cheap one.
        assertTrue(
            requiresToken(
                "POST",
                "/${ApiEndpoints.UPDATE_PROFILE}",
                contentType = "not/a/media/type",
                cookies = cookieAuthenticated
            )
        )
    }

    // --- The parts of the rule that were already there ---

    @Test
    fun `safe methods never need one`() {
        assertFalse(
            requiresToken(
                "GET",
                "/admin/config",
                contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
            )
        )
    }

    @Test
    fun `a header-only bearer request never needs one`() {
        // A browser will not attach Authorization on its own, so nothing here can be forged.
        assertFalse(
            requiresToken(
                "POST",
                "/admin/config",
                contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                authorization = "Bearer any-jwt"
            )
        )
    }

    @Test
    fun `a bearer header alongside a cookie still needs one`() {
        // The cookie is ambient authority whatever else the request carries.
        assertTrue(
            requiresToken(
                "POST",
                "/admin/config",
                contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                authorization = "Bearer any-jwt",
                cookies = cookieAuthenticated
            )
        )
    }
}
