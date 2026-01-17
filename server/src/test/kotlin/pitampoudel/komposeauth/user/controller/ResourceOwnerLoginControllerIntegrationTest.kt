package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.otp.entity.Otp
import pitampoudel.komposeauth.otp.repository.OtpRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.service.UserService

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class ResourceOwnerLoginControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var otpRepository: OtpRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    private fun createUser(email: String, password: String = "Password1") =
        userService.createUser(
            baseUrl = null,
            req = CreateUserRequest(
                firstName = "Test",
                lastName = "User",
                email = email,
                password = password,
                confirmPassword = password
            )
        ).id.toHexString()

    @Test
    fun `login with valid credentials returns profile as cookie`() {
        val email = "login-cookie@example.com"
        val password = "Password1"
        createUser(email, password)

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = password)
            )
        }.andExpect {
            status { isOk() }
            cookie { exists(Constants.ACCESS_TOKEN_COOKIE_NAME) }
            content {
                jsonPath("$.email") { value(email) }
                jsonPath("$.givenName") { exists() }
            }
        }
    }

    @Test
    fun `login with valid credentials returns tokens as TOKEN response type`() {
        val email = "login-token@example.com"
        val password = "Password1"
        createUser(email, password)

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.TOKEN.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = password)
            )
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.access_token") { exists() }
                jsonPath("$.refresh_token") { exists() }
                jsonPath("$.token_type") { value("Bearer") }
                jsonPath("$.expires_in") { exists() }
            }
        }
    }

    @Test
    fun `login with invalid password fails`() {
        val email = "login-invalid@example.com"
        createUser(email, "Password1")

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = "WrongPassword1")
            )
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `login with non-existent user fails`() {
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = "nonexistent@example.com", password = "Password1")
            )
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `login response includes KYC verification status`() {
        val email = "login-kyc@example.com"
        val password = "Password1"
        createUser(email, password)

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = password)
            )
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.kycVerified") { exists() }
                jsonPath("$.kycVerified") { isBoolean() }
            }
        }
    }

    @Test
    fun `login uses COOKIE response type by default`() {
        val email = "login-default@example.com"
        val password = "Password1"
        createUser(email, password)

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = password)
            )
        }.andExpect {
            status { isOk() }
            cookie { exists(Constants.ACCESS_TOKEN_COOKIE_NAME) }
        }
    }

    @Test
    fun `login with deactivated account fails`() {
        val email = "login-deactivated@example.com"
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, createUser(email))

        // Deactivate the account
        mockMvc.post("/${ApiEndpoints.DEACTIVATE}") {
            accept = MediaType.APPLICATION_JSON
            this.cookie(cookie)
        }.andExpect {
            status { isOk() }
        }

        // Try to login again
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = "Password1")
            )
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `otp login creates new user when username is unknown`() {
        val email = "otp-login-new@example.com"
        val otp = "123456"
        otpRepository.save(Otp(receiver = email, otp = otp))

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.OTP(username = email, otp = otp)
            )
        }.andExpect {
            status { isOk() }
            cookie { exists(Constants.ACCESS_TOKEN_COOKIE_NAME) }
            content {
                jsonPath("$.email") { value(email) }
                jsonPath("$.emailVerified") { value(true) }
            }
        }

        val created = userRepository.findByEmail(email)
        assertNotNull(created)
        assertEquals(true, created!!.emailVerified)
    }
}
