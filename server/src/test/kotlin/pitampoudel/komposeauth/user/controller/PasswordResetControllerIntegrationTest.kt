package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.repository.OneTimeTokenRepository
import java.time.Instant
import kotlin.time.Duration.Companion.days

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class PasswordResetControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var oneTimeTokenRepository: OneTimeTokenRepository

    @Test
    fun `sendResetLink succeeds for existing user email`() {
        val email = "reset-link@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)

        mockMvc.put("/${ApiEndpoints.RESET_PASSWORD}") {
            accept = MediaType.APPLICATION_JSON
            param("email", email)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { exists() }
            }
        }
    }

    @Test
    fun `sendResetLink returns error for non-existent email`() {
        mockMvc.put("/${ApiEndpoints.RESET_PASSWORD}") {
            accept = MediaType.APPLICATION_JSON
            param("email", "nonexistent@example.com")
        }.andExpect {
            status { isBadRequest() }
            content {
                jsonPath("$.message") { value("No user with that email") }
            }
        }
    }

    @Test
    fun `resetPasswordForm displays form with valid token`() {
        val email = "reset-form@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "reset-form-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            expiresAt = Instant.now() + 1.days.toJavaDuration(),
            consumed = false
        )
        oneTimeTokenRepository.save(token)

        mockMvc.get("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token.token)
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
        
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "reset-pwd-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            expiresAt = Instant.now() + 1.days.toJavaDuration(),
            consumed = false
        )
        oneTimeTokenRepository.save(token)

        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token.token)
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { is3xxRedirection() }
        }

        // Verify user can login with new password
        TestAuthHelpers.loginCookie(mockMvc, json, email, "NewPassword1")
    }

    @Test
    fun `resetPassword fails with expired token`() {
        val email = "reset-expired@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "reset-expired-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            expiresAt = Instant.now().minusSeconds(3600), // Expired 1 hour ago
            consumed = false
        )
        oneTimeTokenRepository.save(token)

        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token.token)
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `resetPassword fails with already consumed token`() {
        val email = "reset-consumed@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "reset-consumed-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            expiresAt = Instant.now() + 1.days.toJavaDuration(),
            consumed = true
        )
        oneTimeTokenRepository.save(token)

        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token.token)
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { is4xxClientError() }
        }
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
