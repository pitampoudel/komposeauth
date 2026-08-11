package pitampoudel.komposeauth.core.security.ratelimit

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
import kotlin.test.assertTrue

/**
 * The endpoint operators use to configure abuse limits from observation rather than from a hosting
 * provider's documentation, which is often incomplete and sometimes contradicts itself.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class ClientIpDiagnosticIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `reports the headers the request actually carried`() {
        val cookie = TestAuthHelpers
            .createAdminAndLogin(mockMvc, json, userRepository, "client-ip-admin@example.com").second

        val body = mockMvc.get("/admin/client-ip") {
            cookie(cookie)
            accept = MediaType.APPLICATION_JSON
            header("X-Forwarded-For", "198.51.100.7, 10.0.0.9")
            header("X-Real-IP", "198.51.100.7")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString

        // The point of the endpoint: show what arrived, so the operator can pick the header that
        // holds the address they recognise.
        assertTrue(body.contains("X-Forwarded-For"), body)
        assertTrue(body.contains("X-Real-IP"), body)
        assertTrue(body.contains("countedAs"), body)
        assertTrue(body.contains("decidedBy"), body)
    }

    @Test
    fun `omits headers that were not sent`() {
        val cookie = TestAuthHelpers
            .createAdminAndLogin(mockMvc, json, userRepository, "client-ip-absent@example.com").second

        val body = mockMvc.get("/admin/client-ip") {
            cookie(cookie)
            accept = MediaType.APPLICATION_JSON
            header("X-Real-IP", "198.51.100.7")
        }.andReturn().response.contentAsString

        // Listing every candidate name with a blank value would suggest the platform sets them all.
        assertTrue(!body.contains("Fly-Client-IP"), body)
    }

    @Test
    fun `is not readable by an ordinary signed-in user`() {
        // It describes the request chain. Not secret, but not everyone's business either.
        TestAuthHelpers.createUser(mockMvc, json, "client-ip-plain@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "client-ip-plain@example.com")

        val status = mockMvc.get("/admin/client-ip") {
            cookie(cookie)
            accept = MediaType.APPLICATION_JSON
        }.andReturn().response.status

        assertTrue(status !in 200..299, "expected refusal for a non-admin, got $status")
    }

    @Test
    fun `is not readable anonymously`() {
        val status = mockMvc.get("/admin/client-ip") {
            accept = MediaType.APPLICATION_JSON
        }.andReturn().response.status

        assertTrue(status !in 200..299, "expected refusal without a session, got $status")
    }
}
