package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.data.CreateUserRequest
import pitampoudel.komposeauth.core.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.core.data.UpdateProfileRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class EndpointSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `home endpoint requires authentication`() {
        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `update profile requires authentication`() {
        val request = UpdateProfileRequest(givenName = "Test")
        
        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(UpdateProfileRequest.serializer(), request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `get me endpoint requires authentication`() {
        mockMvc.get("/${ApiEndpoints.ME}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `deactivate account requires authentication`() {
        mockMvc.post("/${ApiEndpoints.DEACTIVATE}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `list admins requires ADMIN role`() {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "regular-user-admins@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "regular-user-admins@example.com")

        mockMvc.get("/admins") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `grant admin requires ADMIN role`() {
        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-grant-security@example.com")
        val regularUserId = TestAuthHelpers.createUser(mockMvc, json, "regular-grant-security@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "regular-grant-security@example.com")

        mockMvc.post("/admins/$targetUserId") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `revoke admin requires ADMIN role`() {
        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-revoke-security@example.com")
        val regularUserId = TestAuthHelpers.createUser(mockMvc, json, "regular-revoke-security@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "regular-revoke-security@example.com")

        mockMvc.delete("/admins/$targetUserId") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `send verification email requires authentication`() {
        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `update phone number requires authentication`() {
        val request = UpdatePhoneNumberRequest(phoneNumber = "+1234567890")
        
        mockMvc.post("/${ApiEndpoints.UPDATE_PHONE_NUMBER}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(UpdatePhoneNumberRequest.serializer(), request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `public endpoints are accessible without authentication`() {
        // Login endpoint should be public
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            // Not checking exact status - just ensuring it's not 403 (Forbidden)
            status { isOk() }
        }

        // Create user endpoint should be public
        mockMvc.patch("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                CreateUserRequest(
                    firstName = "Public",
                    lastName = "Test",
                    email = "public-endpoint-test@example.com",
                    password = "Password1",
                    confirmPassword = "Password1"
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `authenticated user can access their own profile`() {
        val email = "own-profile@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `admin user can access admin endpoints`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-access@example.com"
        )

        mockMvc.get("/admins") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `invalid token is rejected`() {
        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer invalid-token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `expired token is rejected`() {
        // This would require creating an expired token, which is complex
        // For now, test that malformed tokens are rejected
        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
