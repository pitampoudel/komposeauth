package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
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
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    /**
     * A client built without [TestConfig]'s default request, so it sends no CSRF token at all —
     * exactly the position of a cross-site caller, which cannot read one.
     */
    private fun clientWithoutCsrfToken(): MockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()

    @Test
    fun `cookie-authenticated write is rejected without a csrf token`() {
        val email = "csrf-reject@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val response = clientWithoutCsrfToken().perform(
            MockMvcRequestBuilders.post("/${ApiEndpoints.UPDATE_PROFILE}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(cookie)
                .content("""{"givenName":"Forged"}""")
        ).andReturn().response

        // Asserted as a range rather than one code on purpose: CsrfFilter runs before the bearer
        // token in the cookie is read, so the principal is still anonymous when the request is
        // refused, and ExceptionTranslationFilter answers an anonymous access denial by starting
        // authentication (401) rather than reporting a forbidden action (403). Which of the two it
        // is doesn't matter here; that the write is refused does.
        assertTrue(
            response.status in 400..499,
            "forged write should be refused, got ${response.status}"
        )

        // The property that actually counts: the request did not change anything.
        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertNotEquals("Forged", user.firstName)
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
