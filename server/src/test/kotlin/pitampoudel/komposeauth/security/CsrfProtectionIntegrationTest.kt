package pitampoudel.komposeauth.security

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockServletContext
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.security.csrf.CrossOriginCsrfTokenRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The rest of the suite gets a CSRF token on every request from [TestConfig], which is what a real
 * browser would have. These tests cover the other half: that a cookie-authenticated, state-changing
 * request without one is actually turned away.
 *
 * This matters because the access-token cookie is deliberately SameSite=None, so it rides along on
 * cross-site requests. Without this protection any page on the internet could drive a write using a
 * signed-in victim's credentials.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class CsrfProtectionIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var securityFilterChainProxy: FilterChainProxy

    /** The matcher the running CsrfFilter actually decides with. */
    private fun liveCsrfMatcher(): RequestMatcher {
        val filter = securityFilterChainProxy.getFilters("/${ApiEndpoints.UPDATE_PROFILE}").orEmpty()
            .filterIsInstance<CsrfFilter>()
            .first()
        return ReflectionTestUtils.getField(filter, "requireCsrfProtectionMatcher") as RequestMatcher
    }

    @Test
    fun `a cookie-authenticated write still requires a csrf token`() {
        // Guards a specific regression. OAuth2ResourceServerConfigurer registers a CSRF exemption for
        // any request its BearerTokenResolver can pull a token from, and this application's resolver
        // falls back to the access-token cookie — which silently exempted every cookie-authenticated
        // request, the exact case CSRF is here to cover. Asserting against the live matcher catches
        // that coming back even if the end-to-end request below is ever weakened.
        val cookieAuthenticated = MockMvcRequestBuilders
            .post("/${ApiEndpoints.UPDATE_PROFILE}")
            .cookie(Cookie(ACCESS_TOKEN_COOKIE_NAME, "any-jwt"))
            .buildRequest(MockServletContext())

        assertTrue(
            liveCsrfMatcher().matches(cookieAuthenticated),
            "a cookie-authenticated write must require a CSRF token"
        )
    }

    @Test
    fun `a header-only bearer request stays exempt`() {
        // Native and server-to-server clients authenticate with the header, which a browser will not
        // attach cross-site, so demanding a token there would break them for no security gain.
        val headerAuthenticated = MockMvcRequestBuilders
            .post("/${ApiEndpoints.UPDATE_PROFILE}")
            .header("Authorization", "Bearer any-jwt")
            .buildRequest(MockServletContext())

        assertFalse(
            liveCsrfMatcher().matches(headerAuthenticated),
            "a header-only bearer request should not need a CSRF token"
        )
    }

    @Test
    fun `cookie-authenticated write is rejected without a csrf token`() {
        val email = "csrf-reject@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        // Opts out of the harness's automatic token, so this request carries none at all — the
        // position a cross-site caller is in. Sending a deliberately *invalid* token does not work:
        // csrf() sets the token parameter, so whichever processor runs last decides its value.
        val response = mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            cookie(cookie)
            content = """{"givenName":"Forged"}"""
        }.andReturn().response

        // The property the protection exists for: the forged write must not have landed.
        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertNotEquals(
            "Forged",
            user.firstName,
            "forged write was applied (status ${response.status})"
        )

        assertTrue(
            response.status !in 200..299,
            "forged write should not have succeeded, got ${response.status}"
        )
    }

    @Test
    fun `same write succeeds with a valid csrf token`() {
        val email = "csrf-accept@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = """{"givenName":"Legitimate"}"""
        }.andExpect {
            status { isOk() }
        }

        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertEquals("Legitimate", user.firstName)
    }

    @Test
    fun `logout requires a csrf token`() {
        val email = "csrf-logout@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val response = mockMvc.post("/${ApiEndpoints.LOGOUT}") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            cookie(cookie)
        }.andReturn().response

        assertTrue(
            response.status !in 200..299,
            "a forged logout should not succeed, got ${response.status}"
        )
    }

    /**
     * `/login` stays CSRF-exempt so native clients can sign in without first fetching a token, and
     * is safe only because it will not read a body a cross-site form can produce. That is a property
     * of how the endpoint parses its input rather than of the security configuration, so it is
     * pinned here — otherwise switching to form binding would quietly open login CSRF.
     */
    @Test
    fun `login refuses the body a cross-site form could send`() {
        val response = mockMvc.post("/${ApiEndpoints.LOGIN}") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            param("username", "victim@example.com")
            param("password", "Password1")
        }.andReturn().response

        assertEquals(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            response.status,
            "a form-encoded login must be refused outright"
        )
    }
}
