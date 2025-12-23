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
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.data.UpdateProfileRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class UsersControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `update profile succeeds for authenticated user`() {
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, "update-user@example.com"))

        val request = UpdateProfileRequest(
            givenName = "Updated",
            familyName = "User"
        )

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(UpdateProfileRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.givenName") { value("Updated") }
                jsonPath("$.familyName") { value("User") }
            }
        }
    }

    @Test
    fun `get user profile succeeds for authenticated user`() {
        val email = "profile-user@example.com"
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, email))

        mockMvc.get("/${ApiEndpoints.ME}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.email") { value(email) }
            }
        }
    }

    @Test
    fun `deactivate account succeeds and prevents login`() {
        val email = "deactivate-user@example.com"
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, email))

        mockMvc.post("/${ApiEndpoints.DEACTIVATE}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("User account deactivated successfully") }
            }
        }

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(Credential.serializer(), Credential.UsernamePassword(username = email, password = "Password1"))
        }.andExpect {
            status { isForbidden() }
        }
    }
}