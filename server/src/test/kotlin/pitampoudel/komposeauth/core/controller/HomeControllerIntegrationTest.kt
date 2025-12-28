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
