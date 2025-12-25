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
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import kotlin.time.Duration.Companion.minutes

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
    private lateinit var oneTimeTokenService: OneTimeTokenService





    @Test
    fun `verifyEmail succeeds with valid token`() {
        val email = "verify-token@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)

        // Create a valid one-time token for email verification
        val token = oneTimeTokenService.createToken(
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            ttl = 1.minutes
        )

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
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
}
