package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.hamcrest.Matchers.hasItem
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
import pitampoudel.komposeauth.core.domain.ApiEndpoints.ROLES
import pitampoudel.komposeauth.core.domain.ApiEndpoints.USERS
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class RolesControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun rolePath(userId: String, role: String) = "/$USERS/$userId/$ROLES/$role"

    @Test
    fun `listRoles returns the catalog for ADMIN`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-list-test@example.com"
        )

        mockMvc.get("/$ROLES") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$") { isArray() }
                jsonPath("$[0].role") { value(Roles.ADMIN) }
                jsonPath("$[0].builtIn") { value(true) }
                jsonPath("$[0].userCount") { exists() }
            }
        }
    }

    @Test
    fun `listRoles forbidden for non-admin users`() {
        TestAuthHelpers.createUser(mockMvc, json, "non-admin-role-list@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "non-admin-role-list@example.com")

        mockMvc.get("/$ROLES") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `grant role succeeds for admin`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-grant-test@example.com"
        )

        val targetUserId = TestAuthHelpers.createUser(mockMvc, json, "target-role-grant@example.com")

        mockMvc.post(rolePath(targetUserId, Roles.ADMIN)) {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(targetUserId) }
                jsonPath("$.roles") { value(hasItem(Roles.ADMIN)) }
            }
        }
    }

    @Test
    fun `grant role forbidden for non-admin`() {
        TestAuthHelpers.createUser(mockMvc, json, "non-admin-role-grant@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "non-admin-role-grant@example.com")

        val targetUserId =
            TestAuthHelpers.createUser(mockMvc, json, "target-role-forbidden@example.com")

        mockMvc.post(rolePath(targetUserId, Roles.ADMIN)) {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `grant role rejects a role outside the catalog`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-uncatalogued-actor@example.com"
        )

        val targetUserId =
            TestAuthHelpers.createUser(mockMvc, json, "target-uncatalogued@example.com")

        mockMvc.post(rolePath(targetUserId, "NOT_IN_CATALOG")) {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `grant SUPER_ADMIN is forbidden for a plain admin`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-escalation-actor@example.com"
        )

        val targetUserId =
            TestAuthHelpers.createUser(mockMvc, json, "target-escalation@example.com")

        mockMvc.post(rolePath(targetUserId, Roles.SUPER_ADMIN)) {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `revoke role succeeds for admin`() {
        // Two admins: one performs the action, the other gets demoted.
        val (_, admin1Cookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-revoke1@example.com"
        )

        val (adminId2, _) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-revoke2@example.com"
        )

        mockMvc.delete(rolePath(adminId2, Roles.ADMIN)) {
            accept = MediaType.APPLICATION_JSON
            cookie(admin1Cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(adminId2) }
                jsonPath("$.roles") { isEmpty() }
            }
        }
    }

    @Test
    fun `revoke role forbidden for non-admin`() {
        val (adminId, _) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-to-revoke@example.com"
        )

        TestAuthHelpers.createUser(mockMvc, json, "non-admin-role-revoke@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "non-admin-role-revoke@example.com")

        mockMvc.delete(rolePath(adminId, Roles.ADMIN)) {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `list users filtered by role`() {
        val (adminId, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "role-filter-admin@example.com"
        )

        mockMvc.get("/$USERS") {
            accept = MediaType.APPLICATION_JSON
            param("role", Roles.ADMIN)
            param("page", "0")
            param("size", "50")
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.items") { isArray() }
                // The filtered page actually contains the admin we just created.
                jsonPath("$.items[?(@.id == '$adminId')].id") { value(hasItem(adminId)) }
                jsonPath("$.page") { value(0) }
                jsonPath("$.pageSize") { value(50) }
            }
        }
    }
}
