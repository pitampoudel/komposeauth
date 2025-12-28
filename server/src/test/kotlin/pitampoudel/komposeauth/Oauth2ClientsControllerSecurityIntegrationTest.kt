package pitampoudel.komposeauth

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class Oauth2ClientsControllerSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `oauth2 clients endpoints require admin - normal user gets 403`() {
        TestAuthHelpers.createUser(mockMvc, json, "normal-oauth-client@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "normal-oauth-client@example.com")

        mockMvc.get("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }

        val request = CreateClientRequest(
            clientName = "Demo",
            clientId = "demo-client",
            clientSecret = "secret",
            redirectUris = setOf("https://example.com/callback"),
            scopes = setOf("openid")
        )

        mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

}
