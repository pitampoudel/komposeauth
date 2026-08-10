package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertNotEquals
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

    @Test
    fun `cookie-authenticated write is rejected without a valid csrf token`() {
        val email = "csrf-reject@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        // The same fully-wired client the rest of the suite uses, so the chain under test is the
        // real one — but opting out of the automatic token, so this request carries none at all.
        //
        // An earlier version sent a deliberately invalid token instead. That does not work: csrf()
        // *sets* the token parameter, so the harness's own token processor simply overwrote the bad
        // value with a good one and the write sailed through, looking exactly like a security hole.
        // Sending nothing cannot be undone by ordering.
        val response = mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            cookie(cookie)
            content = """{"givenName":"Forged"}"""
        }.andReturn().response

        // Checked first, because this is the property the protection exists for: whatever status
        // the refusal is dressed up as, the forged write must not have landed.
        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertNotEquals(
            "Forged",
            user.firstName,
            "forged write was applied — CSRF is not protecting this endpoint " +
                    "(response status was ${response.status})"
        )

        // Deliberately only "not success". CsrfFilter runs before the bearer token in the cookie is
        // read, so the principal is still anonymous when the request is refused, and
        // ExceptionTranslationFilter turns an anonymous access denial into "start authentication" —
        // which lands as a 401 or, for a client the entry point decides to redirect, a 302. Pinning
        // an exact code here tests Spring's error plumbing rather than this application's security.
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
        assertNotEquals("Forged", user.firstName)
    }
}
