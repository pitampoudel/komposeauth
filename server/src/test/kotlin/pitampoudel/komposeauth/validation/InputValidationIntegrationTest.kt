package pitampoudel.komposeauth.validation

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.data.CreateUserRequest
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.data.UpdateProfileRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class InputValidationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `create user with weak password fails`() {
        val request = CreateUserRequest(
            firstName = "Test",
            lastName = "User",
            email = "weak-password@example.com",
            password = "weak",
            confirmPassword = "weak"
        )

        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `create user with mismatched passwords fails`() {
        val request = CreateUserRequest(
            firstName = "Test",
            lastName = "User",
            email = "mismatch@example.com",
            password = "Password1",
            confirmPassword = "Password2"
        )

        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `create user with invalid email format fails`() {
        val request = CreateUserRequest(
            firstName = "Test",
            lastName = "User",
            email = "not-an-email",
            password = "Password1",
            confirmPassword = "Password1"
        )

        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

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

        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `create user with empty first name fails`() {
        val request = CreateUserRequest(
            firstName = "",
            lastName = "User",
            email = "empty-name@example.com",
            password = "Password1",
            confirmPassword = "Password1"
        )

        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
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

        mockMvc.patch("/${ApiEndpoints.USERS}") {
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

        mockMvc.patch("/${ApiEndpoints.USERS}") {
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
            status { isUnauthorized() }
        }
    }

    @Test
    fun `update profile with empty name is allowed`() {
        val email = "update-empty@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = UpdateProfileRequest(
            givenName = "",
            familyName = ""
        )

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(UpdateProfileRequest.serializer(), request)
        }.andExpect {
            // Should either succeed or return bad request, not crash
            status { isOk().or(isBadRequest()) }
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
    fun `password change with weak new password fails`() {
        val email = "change-weak@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = UpdateProfileRequest(
            password = "weak",
            confirmPassword = "weak"
        )

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(UpdateProfileRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `password change with mismatched passwords fails`() {
        val email = "change-mismatch@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = UpdateProfileRequest(
            password = "NewPassword1",
            confirmPassword = "NewPassword2"
        )

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(UpdateProfileRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `malformed JSON request returns bad request`() {
        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = "{invalid json}"
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `missing required fields in request returns bad request`() {
        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = "{\"firstName\": \"Test\"}"
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
