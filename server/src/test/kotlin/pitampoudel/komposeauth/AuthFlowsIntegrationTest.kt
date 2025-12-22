package pitampoudel.komposeauth

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertThrows
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
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.repository.OneTimeTokenRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class AuthFlowsIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var oneTimeTokenService: OneTimeTokenService

    @Autowired
    private lateinit var oneTimeTokenRepository: OneTimeTokenRepository

    private fun createUser(email: String = "jane@example.com"): String {
        return TestAuthHelpers.createUser(mockMvc, json, email, password = "Password1")
    }

    @Test
    fun `login TOKEN response returns access+refresh tokens and refresh token is one-time`() {
        createUser("token-login@example.com")

        val loginResponse = mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.TOKEN.name)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(
                    username = "token-login@example.com",
                    password = "Password1"
                )
            )
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString

        val root = json.parseToJsonElement(loginResponse).jsonObject
        val accessToken = root["access_token"]?.jsonPrimitive?.content
        val refreshToken = root["refresh_token"]?.jsonPrimitive?.content
        assertNotNull(accessToken)
        assertNotNull(refreshToken)
        assertEquals("Bearer", root["token_type"]?.jsonPrimitive?.content)

        // Refresh token should exist in DB and be consumable only once.
        assertTrue(
            oneTimeTokenRepository.findAll()
                .any { it.purpose == OneTimeToken.Purpose.REFRESH_TOKEN })
        oneTimeTokenService.consume(refreshToken, OneTimeToken.Purpose.REFRESH_TOKEN)
        assertThrows(IllegalStateException::class.java) {
            oneTimeTokenService.consume(refreshToken, OneTimeToken.Purpose.REFRESH_TOKEN)
        }
    }

    @Test
    fun `login COOKIE response sets access token cookie and cookie can authenticate to ME`() {
        createUser("cookie-login@example.com")

        val mvcResult = mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(
                    username = "cookie-login@example.com",
                    password = "Password1"
                )
            )
        }.andExpect {
            status { isOk() }
            cookie { exists(ACCESS_TOKEN_COOKIE_NAME) }
        }.andReturn()

        val cookieValue = mvcResult.response.getCookie(ACCESS_TOKEN_COOKIE_NAME)?.value
        assertNotNull(cookieValue)

        mockMvc.get("/${ApiEndpoints.ME}") {
            cookie(Cookie(ACCESS_TOKEN_COOKIE_NAME, cookieValue))
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `ME requires authentication`() {
        mockMvc.get("/${ApiEndpoints.ME}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `verify email consumes token and flips emailVerified`() {
        val userId = createUser("verify-me@example.com")
        val userObjId = org.bson.types.ObjectId(userId)

        val token = oneTimeTokenService.createToken(
            userId = userObjId,
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            ttl = 1.hours
        )

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
        }.andExpect {
            status { is3xxRedirection() }
        }

        val updated = userRepository.findById(org.bson.types.ObjectId(userId)).orElseThrow()
        assertTrue(updated.emailVerified)

        // Token must be consumed (replay fails)
        assertThrows(IllegalStateException::class.java) {
            oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.VERIFY_EMAIL)
        }
    }

    @Test
    fun `reset password consumes token and allows login with new password`() {
        createUser("reset-me@example.com")
        val user = userRepository.findByEmail("reset-me@example.com")!!

        val token = oneTimeTokenService.createToken(
            userId = user.id,
            purpose = OneTimeToken.Purpose.RESET_PASSWORD,
            ttl = 1.hours
        )

        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token)
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { is3xxRedirection() }
        }

        assertThrows(IllegalStateException::class.java) {
            oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.RESET_PASSWORD)
        }

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.TOKEN.name)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(
                    username = "reset-me@example.com",
                    password = "NewPassword1"
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

}
