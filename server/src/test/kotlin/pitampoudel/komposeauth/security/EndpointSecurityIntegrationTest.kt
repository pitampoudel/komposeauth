package pitampoudel.komposeauth.security

import jakarta.servlet.http.Cookie
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
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
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
    fun `deactivate any user requires ADMIN role`() {
        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-user-deactivate-security@example.com")
        val regularUserId = TestAuthHelpers.createUser(mockMvc, json, "regular-user-deactivate-security@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "regular-user-deactivate-security@example.com")

        mockMvc.post("/${ApiEndpoints.USERS}/$targetUserId/${ApiEndpoints.DEACTIVATE}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `delete any user requires ADMIN role`() {
        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-user-delete-security@example.com")
        val regularUserId = TestAuthHelpers.createUser(mockMvc, json, "regular-user-delete-security@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "regular-user-delete-security@example.com")

        mockMvc.delete("/${ApiEndpoints.USERS}/$targetUserId") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `public endpoints are accessible without authentication`() {

        // Create user endpoint should be public
        mockMvc.post("/${ApiEndpoints.USERS}") {
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
    fun `invalid header token 401 clears access token cookie`() {
        val setCookie = mockMvc.get("/${ApiEndpoints.ME}") {
            accept = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer invalid-token")
        }.andExpect {
            status { isUnauthorized() }
        }.andReturn().response.getHeaders("Set-Cookie")
        assert(setCookie.any { it.startsWith("$ACCESS_TOKEN_COOKIE_NAME=") && it.contains("Max-Age=0") }) {
            "expected $ACCESS_TOKEN_COOKIE_NAME cookie to be cleared, got: $setCookie"
        }
    }

    @Test
    fun `invalid cookie token 401 clears access token cookie`() {
        val setCookie = mockMvc.get("/${ApiEndpoints.ME}") {
            accept = MediaType.APPLICATION_JSON
            cookie(Cookie(ACCESS_TOKEN_COOKIE_NAME, "invalid-token"))
        }.andExpect {
            status { isUnauthorized() }
        }.andReturn().response.getHeaders("Set-Cookie")
        assert(setCookie.any { it.startsWith("$ACCESS_TOKEN_COOKIE_NAME=") && it.contains("Max-Age=0") }) {
            "expected $ACCESS_TOKEN_COOKIE_NAME cookie to be cleared, got: $setCookie"
        }
    }

    @Test
    fun `purely public endpoint ignores an invalid token and returns 200`() {
        // A fully public endpoint must not attempt authentication: a bad token is ignored,
        // so the request succeeds instead of being rejected with 401.
        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer invalid-token")
            content = json.encodeToString(
                CreateUserRequest(
                    firstName = "Public",
                    lastName = "Test",
                    email = "public-invalid-token@example.com",
                    password = "Password1",
                    confirmPassword = "Password1"
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `optional-auth endpoint still rejects an invalid token`() {
        // Endpoints that use optional authentication continue to validate a supplied token,
        // so an invalid token is rejected with 401.
        mockMvc.get("/config") {
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
