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
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants
import pitampoudel.komposeauth.core.domain.ResponseType

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class ResourceOwnerLoginControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `login with valid credentials returns profile as cookie`() {
        val email = "login-cookie@example.com"
        val password = "Password1"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

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
        TestAuthHelpers.createUser(mockMvc, json, email, password)

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
                jsonPath("$.accessToken") { exists() }
                jsonPath("$.refreshToken") { exists() }
                jsonPath("$.tokenType") { value("Bearer") }
                jsonPath("$.expiresIn") { exists() }
            }
        }
    }

    @Test
    fun `login with invalid password fails`() {
        val email = "login-invalid@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, "Password1")

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = "WrongPassword1")
            )
        }.andExpect {
            status { isUnauthorized() }
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
            status { isUnauthorized() }
        }
    }

    @Test
    fun `login response includes KYC verification status`() {
        val email = "login-kyc@example.com"
        val password = "Password1"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

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
        TestAuthHelpers.createUser(mockMvc, json, email, password)

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
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, email))

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
            status { isUnauthorized() }
        }
    }
}
