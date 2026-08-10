package pitampoudel.komposeauth.core.controller

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.repository.UserRepository

/**
 * Renders every page of the admin console.
 *
 * They all share one Thymeleaf layout, so a broken fragment parameter or a missing model attribute
 * only shows up when the template is actually processed — which is what these assertions force.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AdminPagesIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun adminCookie(email: String) =
        TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, email).second

    @Test
    fun `overview renders inside the shell`() {
        val cookie = adminCookie("admin-overview@example.com")

        mockMvc.get("/admin") { cookie(cookie) }
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith("text/html") }
                // Shell chrome plus the page's own thesis line.
                content { string(org.hamcrest.Matchers.containsString("Skip to content")) }
                content { string(org.hamcrest.Matchers.containsString("Ways in")) }
                content { string(org.hamcrest.Matchers.containsString("can sign in")) }
            }
    }

    @Test
    fun `people page renders and marks its nav entry current`() {
        val cookie = adminCookie("admin-people@example.com")

        mockMvc.get("/admin/users") { cookie(cookie) }
            .andExpect {
                status { isOk() }
                content { string(org.hamcrest.Matchers.containsString("People and roles")) }
                content { string(org.hamcrest.Matchers.containsString("""href="/admin/users" aria-current="page"""")) }
            }
    }

    @Test
    fun `applications page renders`() {
        val cookie = adminCookie("admin-apps@example.com")

        mockMvc.get("/admin/clients") { cookie(cookie) }
            .andExpect {
                status { isOk() }
                content { string(org.hamcrest.Matchers.containsString("Apps that can ask for tokens")) }
            }
    }

    @Test
    fun `configuration renders in the shell at the admin address`() {
        mockMvc.get("/admin/config") { param("key", TestConfig.testKey) }
            .andExpect {
                status { isOk() }
                content { string(org.hamcrest.Matchers.containsString("Save configuration")) }
                content { string(org.hamcrest.Matchers.containsString("Skip to content")) }
            }
    }

    /**
     * The configuration page is served at exactly one address. `/config` was the old one, and it
     * was also listed in [pitampoudel.komposeauth.core.security.PublicEndpoints.optionalAuthPatterns] —
     * so leaving a second mapping behind would leave a second way past the filter chain too.
     */
    @Test
    fun `configuration answers on the console address only`() {
        val cookie = adminCookie("admin-legacy@example.com")

        mockMvc.get("/config") { cookie(cookie) }
            .andExpect { status { isNotFound() } }
    }
}
