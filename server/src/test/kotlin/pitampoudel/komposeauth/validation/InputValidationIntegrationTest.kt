package pitampoudel.komposeauth.validation

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.UpdateProfileRequest

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class InputValidationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `create user with duplicate email fails`() {
        val email = "duplicate@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)

        val request = CreateUserRequest(
            firstName = "Duplicate",
            lastName = "User",
            email = email,
            password = "Password1",
            confirmPassword = "Password1"
        )

        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { is4xxClientError() }
        }
    }
    @Test
    fun `create user with very long name succeeds`() {
        val longName = "A".repeat(100)
        val request = CreateUserRequest(
            firstName = longName,
            lastName = "User",
            email = "long-name@example.com",
            password = "Password1",
            confirmPassword = "Password1"
        )

        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `create user with special characters in name succeeds`() {
        val request = CreateUserRequest(
            firstName = "José",
            lastName = "O'Brien-Smith",
            email = "special-chars@example.com",
            password = "Password1",
            confirmPassword = "Password1"
        )

        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `login with empty credentials fails`() {
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = "", password = "")
            )
        }.andExpect {
            status { is4xxClientError() }
        }
    }


    @Test
    fun `update profile with null values succeeds`() {
        val email = "update-null@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = UpdateProfileRequest()

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(UpdateProfileRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `malformed JSON request returns bad request`() {
        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = "{invalid json}"
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `missing required fields in request returns bad request`() {
        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = "{\"firstName\": \"Test\"}"
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
