package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints

/**
 * The rest of the suite gets a CSRF token on every request from [TestConfig], which is what a real
 * browser would have. These tests check the other half: that a cookie-authenticated, state-changing
 * request without a good token is actually turned away.
 *
 * This matters because the access-token cookie is deliberately SameSite=None, so it rides along on
 * cross-site requests. Without this protection any page on the internet could drive a form-encoded
 * POST using a signed-in victim's credentials.
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

    @Test
    fun `cookie-authenticated write is rejected without a valid csrf token`() {
        val cookie = TestAuthHelpers.createUser(mockMvc, json, "csrf-reject@example.com")
            .let { TestAuthHelpers.loginCookie(mockMvc, json, "csrf-reject@example.com") }

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = """{"givenName":"Forged"}"""
            // Overrides the token the test harness adds by default, standing in for a cross-site
            // caller that cannot read the real one.
            with(csrf().useInvalidToken())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `same write succeeds with a valid csrf token`() {
        TestAuthHelpers.createUser(mockMvc, json, "csrf-accept@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "csrf-accept@example.com")

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = """{"givenName":"Legitimate"}"""
        }.andExpect {
            status { isOk() }
        }
    }
}
