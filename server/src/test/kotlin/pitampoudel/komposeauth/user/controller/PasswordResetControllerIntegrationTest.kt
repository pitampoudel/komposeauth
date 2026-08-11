package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import kotlin.time.Duration.Companion.minutes

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class PasswordResetControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var oneTimeTokenService: OneTimeTokenService


    /**
     * The endpoint is public, so a caller must not be able to tell a registered address from an
     * unregistered one — same status, same message either way.
     */
    @Test
    fun `sendResetLink does not reveal whether the email has an account`() {
        val neutralMessage = "If that address has an account, a reset link is on its way."

        mockMvc.put("/${ApiEndpoints.RESET_PASSWORD}") {
            accept = MediaType.APPLICATION_JSON
            param("email", "nonexistent@example.com")
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value(neutralMessage) }
            }
        }

        val known = "reset-enumeration@example.com"
        TestAuthHelpers.createUser(mockMvc, json, known)

        mockMvc.put("/${ApiEndpoints.RESET_PASSWORD}") {
            accept = MediaType.APPLICATION_JSON
            param("email", known)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value(neutralMessage) }
            }
        }
    }

    @Test
    fun `resetPasswordForm displays form with valid token`() {
        val email = "reset-form@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val token = oneTimeTokenService.createToken(
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            ttl = 1.minutes
        )


        mockMvc.get("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `resetPasswordForm fails with invalid token`() {
        mockMvc.get("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", "invalid-token")
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `resetPassword succeeds with valid token and matching passwords`() {
        val email = "reset-password@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)

        val token = oneTimeTokenService.createToken(
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            ttl = 1.minutes
        )


        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token)
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { is3xxRedirection() }
        }

        // Verify user can login with new password
        TestAuthHelpers.loginCookie(mockMvc, json, email, "NewPassword1")
    }


    @Test
    fun `resetPassword fails with invalid token`() {
        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", "invalid-token")
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
