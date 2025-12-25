package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
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
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.repository.OneTimeTokenRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import org.bson.types.ObjectId
import java.time.Instant
import kotlin.time.Duration.Companion.days

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class EmailVerifyControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var oneTimeTokenRepository: OneTimeTokenRepository

    @Test
    fun `sendVerificationEmail succeeds for authenticated user with email`() {
        val email = "verify-email@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { exists() }
            }
        }
    }

    @Test
    fun `sendVerificationEmail returns 401 for unauthenticated user`() {
        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `verifyEmail succeeds with valid token`() {
        val email = "verify-token@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        
        // Create a valid one-time token for email verification
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "valid-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            expiresAt = Instant.now() + 1.days.toJavaDuration(),
            consumed = false
        )
        oneTimeTokenRepository.save(token)

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token.token)
        }.andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `verifyEmail fails with invalid token`() {
        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", "invalid-token")
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `verifyEmail fails with expired token`() {
        val email = "verify-expired@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        
        // Create an expired token
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "expired-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            expiresAt = Instant.now().minusSeconds(3600), // Expired 1 hour ago
            consumed = false
        )
        oneTimeTokenRepository.save(token)

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token.token)
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `verifyEmail fails with already consumed token`() {
        val email = "verify-consumed@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        
        // Create a consumed token
        val token = OneTimeToken(
            id = ObjectId.get(),
            token = "consumed-token-${System.currentTimeMillis()}",
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            expiresAt = Instant.now() + 1.days.toJavaDuration(),
            consumed = true
        )
        oneTimeTokenRepository.save(token)

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token.token)
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
