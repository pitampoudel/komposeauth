package pitampoudel.komposeauth

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.LogoutRequest
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.repository.OneTimeTokenRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
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
        assertThrows(ResponseStatusException::class.java) {
            oneTimeTokenService.consume(refreshToken, OneTimeToken.Purpose.REFRESH_TOKEN)
        }
    }

    private fun tokenLogin(credential: Credential): MockHttpServletResponse = mockMvc.post("/${ApiEndpoints.LOGIN}") {
        param("responseType", ResponseType.TOKEN.name)
        contentType = MediaType.APPLICATION_JSON
        content = json.encodeToString<Credential>(credential)
    }.andReturn().response

    private fun refreshTokenOf(response: MockHttpServletResponse): String {
        assertEquals(200, response.status, response.contentAsString)
        return assertNotNull(json.parseToJsonElement(response.contentAsString).jsonObject["refresh_token"]).jsonPrimitive.content
    }

    @Test
    fun `a refresh hands out a new refresh token, and replaying the old one ends both`() {
        createUser("rotation@example.com")
        val first = refreshTokenOf(tokenLogin(Credential.UsernamePassword(username = "rotation@example.com", password = "Password1")))
        val second = refreshTokenOf(tokenLogin(Credential.RefreshToken(first)))
        assertNotEquals(first, second)

        assertEquals(400, tokenLogin(Credential.RefreshToken(first)).status)
        // Either of the two holders may be the one who copied it, so the newer token goes as well.
        assertEquals(400, tokenLogin(Credential.RefreshToken(second)).status)
    }

    @Test
    fun `a refresh token is refused unspent when the response could not carry its successor`() {
        createUser("refresh-cookie@example.com")
        val first = refreshTokenOf(tokenLogin(Credential.UsernamePassword(username = "refresh-cookie@example.com", password = "Password1")))

        listOf(ResponseType.COOKIE, ResponseType.SESSION).forEach { responseType ->
            val refused = mockMvc.post("/${ApiEndpoints.LOGIN}") {
                param("responseType", responseType.name)
                contentType = MediaType.APPLICATION_JSON
                content = json.encodeToString<Credential>(Credential.RefreshToken(first))
            }.andReturn().response
            assertEquals(400, refused.status, refused.contentAsString)
            assertTrue(refused.getHeaders("Set-Cookie").none { it.startsWith(ACCESS_TOKEN_COOKIE_NAME) })
        }

        refreshTokenOf(tokenLogin(Credential.RefreshToken(first)))
    }

    @Test
    fun `a replay ends only the sign-in it descends from`() {
        createUser("two-devices@example.com")
        val password = Credential.UsernamePassword(username = "two-devices@example.com", password = "Password1")
        val phone = refreshTokenOf(tokenLogin(password))
        val laptop = refreshTokenOf(tokenLogin(password))

        val phoneNext = refreshTokenOf(tokenLogin(Credential.RefreshToken(phone)))
        assertEquals(400, tokenLogin(Credential.RefreshToken(phone)).status)
        assertEquals(400, tokenLogin(Credential.RefreshToken(phoneNext)).status)

        refreshTokenOf(tokenLogin(Credential.RefreshToken(laptop)))
    }

    private fun logout(body: String?): MockHttpServletResponse = mockMvc.post("/${ApiEndpoints.LOGOUT}") {
        if (body != null) {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }
    }.andReturn().response

    @Test
    fun `signing out with the refresh token ends it and the sign-in it was refreshed from`() {
        createUser("signout-family@example.com")
        val password = Credential.UsernamePassword(username = "signout-family@example.com", password = "Password1")
        val first = refreshTokenOf(tokenLogin(password))
        val second = refreshTokenOf(tokenLogin(Credential.RefreshToken(first)))
        val otherDevice = refreshTokenOf(tokenLogin(password))

        assertEquals(200, logout(json.encodeToString(LogoutRequest(refreshToken = second))).status)

        assertEquals(400, tokenLogin(Credential.RefreshToken(second)).status)
        refreshTokenOf(tokenLogin(Credential.RefreshToken(otherDevice)))
    }

    @Test
    fun `a sign-out holding a token already refreshed still ends the one that replaced it`() {
        createUser("logout-stale@example.com")
        val first = refreshTokenOf(tokenLogin(Credential.UsernamePassword(username = "logout-stale@example.com", password = "Password1")))
        val second = refreshTokenOf(tokenLogin(Credential.RefreshToken(first)))

        assertEquals(200, logout(json.encodeToString(LogoutRequest(refreshToken = first))).status)

        assertEquals(400, tokenLogin(Credential.RefreshToken(second)).status)
    }

    @Test
    fun `a sign-out without a refresh token, or with one nobody issued, still succeeds`() {
        assertEquals(200, logout(null).status)
        assertEquals(200, logout("{}").status)
        assertEquals(200, logout("not json").status)
        assertEquals(200, logout(json.encodeToString(LogoutRequest(refreshToken = "never-issued"))).status)
    }

    @Test
    fun `login COOKIE response sets access token cookie and cookie can authenticate to ME`() {
        val userId = org.bson.types.ObjectId(createUser("cookie-login@example.com"))

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
        // Nobody receives a refresh token on a cookie sign-in, so none is minted.
        assertTrue(
            oneTimeTokenRepository.findAll()
                .none { it.userId == userId && it.purpose == OneTimeToken.Purpose.REFRESH_TOKEN }
        )

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
        val email = "verify-me@example.com"
        val userId = createUser(email)
        val userObjId = org.bson.types.ObjectId(userId)

        // Issued the way the application issues it, so the address the link speaks for is recorded
        // on the token. A hand-built token names no address and is refused as a stale link.
        val token = oneTimeTokenService.generateEmailVerificationLink(
            userId = userObjId,
            ttl = 1.hours,
            baseUrl = "http://localhost",
            email = email
        ).substringAfter("token=")

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
        }.andExpect {
            status { is3xxRedirection() }
        }

        val updated = userRepository.findById(org.bson.types.ObjectId(userId)).orElseThrow()
        assertTrue(updated.emailVerified)

        // Token must be consumed (replay fails)
        assertThrows(ResponseStatusException::class.java) {
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
        val refreshTokenBeforeReset = oneTimeTokenService.generateRefreshToken(user.id)

        mockMvc.post("/${ApiEndpoints.RESET_PASSWORD}") {
            param("token", token)
            param("newPassword", "NewPassword1")
            param("confirmPassword", "NewPassword1")
        }.andExpect {
            status { isOk() }
        }

        assertThrows(ResponseStatusException::class.java) {
            oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.RESET_PASSWORD)
        }
        // A reset is how a stolen account is taken back, so whatever was signed in before is out.
        assertThrows(ResponseStatusException::class.java) {
            oneTimeTokenService.findValidToken(refreshTokenBeforeReset, OneTimeToken.Purpose.REFRESH_TOKEN)
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
