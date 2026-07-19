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
import org.springframework.test.web.servlet.patch
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
    fun `create user with very long name succeeds`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, pitampoudel.komposeauth.user.repository.UserRepository::class.java.let { mockMvc.dispatcherServlet.webApplicationContext?.getBean(it)!! }, "admin-longname@example.com")
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
            cookie(adminCookie)
            content = json.encodeToString(CreateUserRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `create user with special characters in name succeeds`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, pitampoudel.komposeauth.user.repository.UserRepository::class.java.let { mockMvc.dispatcherServlet.webApplicationContext?.getBean(it)!! }, "admin-specialchars@example.com")
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
            cookie(adminCookie)
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
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, pitampoudel.komposeauth.user.repository.UserRepository::class.java.let { mockMvc.dispatcherServlet.webApplicationContext?.getBean(it)!! }, "admin-malformed@example.com")
        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = "{invalid json}"
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `missing required fields in request returns bad request`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, pitampoudel.komposeauth.user.repository.UserRepository::class.java.let { mockMvc.dispatcherServlet.webApplicationContext?.getBean(it)!! }, "admin-missing@example.com")
        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = "{\"firstName\": \"Test\"}"
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
