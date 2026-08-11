package pitampoudel.komposeauth.core.controller

import kotlinx.serialization.json.Json
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
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class HomeControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `home endpoint returns profile for authenticated user`() {
        val email = "home-test@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(userId) }
                jsonPath("$.email") { value(email) }
                jsonPath("$.givenName") { value("Test") }
                jsonPath("$.familyName") { value("User") }
            }
        }
    }

    @Test
    fun `home endpoint returns 401 for unauthenticated request`() {
        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    /**
     * A browser asking for the root gets somewhere to be, not the JSON above.
     *
     * This is where a sign-in with nothing to resume lands, and it used to answer with the visitor's
     * own profile as raw fields — on a host they never chose to visit, with nothing to click.
     */
    @Test
    fun `a browser at the root is sent to the console`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "home-html-admin@example.com"
        )

        mockMvc.get("/") {
            accept = MediaType.TEXT_HTML
            cookie(adminCookie)
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/admin")
        }
    }

    /** And a client asking for JSON is unaffected, which is what the split is for. */
    @Test
    fun `a browser at the root is never handed the profile payload`() {
        val email = "home-html-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val result = mockMvc.get("/") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect {
            status { is3xxRedirection() }
        }.andReturn()

        assert(!result.response.contentAsString.contains("givenName")) {
            "the root answered a browser with profile JSON: ${result.response.contentAsString.take(200)}"
        }
    }

    /**
     * A caller with no preference is not a browser, and there are two handlers on this path for it
     * to choose between — the state in which Spring gives up and reports the mapping as ambiguous.
     * A wildcard `Accept` is what a health check, a `curl`, and any client that does not set the
     * header at all sends, so this is the request most likely to arrive and the one least likely to
     * be tried by hand.
     */
    @Test
    fun `a caller with no preference still gets the profile`() {
        val email = "home-any-accept@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.get("/") {
            accept = MediaType.ALL
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
        }
    }

    @Test
    fun `home endpoint works for admin users`() {
        val (adminId, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "home-admin@example.com"
        )

        mockMvc.get("/") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(adminId) }
            }
        }
    }
}
