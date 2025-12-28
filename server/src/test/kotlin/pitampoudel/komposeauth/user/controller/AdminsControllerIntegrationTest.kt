package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AdminsControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `listAdmins returns admins only for ADMIN role`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-list-test@example.com"
        )

        mockMvc.get("/admins") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.items") { isArray() }
                jsonPath("$.page") { exists() }
                jsonPath("$.totalItems") { exists() }
            }
        }
    }

    @Test
    fun `listAdmins forbidden for non-admin users`() {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "non-admin-list@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "non-admin-list@example.com")

        mockMvc.get("/admins") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `grant admin role succeeds for admin`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-grant-test@example.com"
        )

        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-grant@example.com")

        mockMvc.post("/admins/$targetUserId") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(targetUserId) }
            }
        }
    }

    @Test
    fun `grant admin role forbidden for non-admin`() {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "non-admin-grant@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "non-admin-grant@example.com")

        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-forbidden@example.com")

        mockMvc.post("/admins/$targetUserId") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `revoke admin role succeeds for admin`() {
        // Create two admins - one to perform action, one to revoke
        val (adminId1, admin1Cookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-revoke1@example.com"
        )

        val (adminId2, _) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-revoke2@example.com"
        )

        mockMvc.delete("/admins/$adminId2") {
            accept = MediaType.APPLICATION_JSON
            cookie(admin1Cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(adminId2) }
            }
        }
    }

    @Test
    fun `revoke admin role forbidden for non-admin`() {
        val (adminId, _) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-to-revoke@example.com"
        )

        val userId = TestAuthHelpers.createUser(mockMvc, json, "non-admin-revoke@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "non-admin-revoke@example.com")

        mockMvc.delete("/admins/$adminId") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `listAdmins supports pagination`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "admin-pagination@example.com"
        )

        mockMvc.get("/admins") {
            accept = MediaType.APPLICATION_JSON
            param("page", "0")
            param("size", "10")
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.items") { isArray() }
                jsonPath("$.page") { value(0) }
                jsonPath("$.pageSize") { value(10) }
            }
        }
    }
}
