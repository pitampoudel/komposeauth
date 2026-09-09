package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertTrue

/**
 * A SUPER_ADMIN holds everything an ADMIN holds.
 *
 * Without the hierarchy the two were unrelated sets, and the admin console showed it: `/admin/users`
 * admits either role, but the `/users` call the page makes to fill itself in asks only for ADMIN. A
 * super admin could open the user list and watch it fail while a plain admin saw it fine.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class RoleHierarchyIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun loginWithRoles(email: String, vararg roles: String) = run {
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        userRepository.save(user.copy(roles = roles.toList()))
        TestAuthHelpers.loginCookie(mockMvc, json, email)
    }

    @Test
    fun `a super admin can read the user list`() {
        val cookie = loginWithRoles("hierarchy-super@example.com", Roles.SUPER_ADMIN)

        mockMvc.get("/${ApiEndpoints.USERS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `a super admin can read the roles catalog`() {
        val cookie = loginWithRoles("hierarchy-super-roles@example.com", Roles.SUPER_ADMIN)

        mockMvc.get("/${ApiEndpoints.ROLES}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `the hierarchy does not run the other way`() {
        // ADMIN must not inherit SUPER_ADMIN: that role gates the configuration page and every
        // secret on it, which is the whole reason the two are separate.
        val cookie = loginWithRoles("hierarchy-admin@example.com", Roles.ADMIN)

        val response = mockMvc.get("/admin/config") {
            cookie(cookie)
        }.andReturn().response

        assertTrue(
            response.status in 400..499,
            "an ADMIN must not reach the configuration page, got ${response.status}"
        )
    }

    @Test
    fun `an ordinary user still cannot read the user list`() {
        val cookie = loginWithRoles("hierarchy-plain@example.com")

        mockMvc.get("/${ApiEndpoints.USERS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }
}
