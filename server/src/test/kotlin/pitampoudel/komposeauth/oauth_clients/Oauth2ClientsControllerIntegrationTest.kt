package pitampoudel.komposeauth.oauth_clients

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class Oauth2ClientsControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `admin can create update delete and list oauth2 clients`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "oauth-admin@example.com"
        )

        val createRequest = CreateClientRequest(
            clientName = "Test Client",
            redirectUris = setOf("https://example.com/callback")
        )

        val createResult = mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = json.encodeToString(CreateClientRequest.serializer(), createRequest)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.clientName") { value("Test Client") }
            }
        }.andReturn()

        val createdClientId = json.parseToJsonElement(createResult.response.contentAsString)
            .jsonObject["clientId"]!!.jsonPrimitive.content

        val updateRequest = CreateClientRequest(
            clientId = createdClientId,
            clientName = "Updated Client",
            clientSecret = "manual-secret",
            redirectUris = setOf("https://example.com/updated-callback"),
            scopes = setOf("openid", "email", "user.read.any"),
            clientUri = "https://example.com/app",
            logoUri = "https://example.com/logo.png"
        )

        mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = json.encodeToString(updateRequest)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.clientName") { value("Updated Client") }
                jsonPath("$.clientSecret") { value("manual-secret") }
                jsonPath("$.redirectUris[0]") { value("https://example.com/updated-callback") }
            }
        }

        mockMvc.get("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$[0].clientName") { value("Updated Client") }
            }
        }

        mockMvc.delete("/${ApiEndpoints.OAUTH2_CLIENTS}/$createdClientId") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                json("[]")
            }
        }
    }

    @Test
    fun `admin can load oauth2 clients management ui`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc,
            json,
            userRepository,
            "oauth-ui-admin@example.com"
        )

        mockMvc.get("/oauth2/clients/dashboard") {
            accept = MediaType.TEXT_HTML
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content { string(org.hamcrest.Matchers.containsString("OAuth2 Clients")) }
        }
    }
}
