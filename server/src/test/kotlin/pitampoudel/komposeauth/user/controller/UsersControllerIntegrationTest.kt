package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class UsersControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

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
    fun `admin can deactivate any user`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-deactivate-any-user@example.com"
        )
        val targetEmail = "deactivate-target@example.com"
        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, targetEmail)

        mockMvc.post("/${ApiEndpoints.USERS}/$targetUserId/${ApiEndpoints.DEACTIVATE}") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("User account deactivated successfully") }
            }
        }

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(Credential.serializer(), Credential.UsernamePassword(username = targetEmail, password = "Password1"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `admin can delete any user`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-delete-any-user@example.com"
        )
        val targetEmail = "delete-target@example.com"
        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, targetEmail)

        mockMvc.delete("/${ApiEndpoints.USERS}/$targetUserId") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("User account deleted successfully") }
            }
        }

        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(Credential.serializer(), Credential.UsernamePassword(username = targetEmail, password = "Password1"))
        }.andExpect {
            status { isForbidden() }
        }
    }
}
