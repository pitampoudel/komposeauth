package pitampoudel.komposeauth

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class Oauth2ClientsControllerSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var clientRepository: OAuth2ClientRepository

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
            scopes = setOf("openid"),
            accessTokenTtlSeconds = 900,
            refreshTokenTtlDays = 30
        )

        mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(request)
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.delete("/${ApiEndpoints.OAUTH2_CLIENTS}/demo-client") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.get("/admin/clients") {
            accept = MediaType.TEXT_HTML
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    /**
     * Saving over a client without its secret issues a new one, which stops the app holding the old
     * one from signing anybody in, so an ADMIN may look at the clients but only a SUPER_ADMIN may
     * change them.
     */
    @Test
    fun `a plain admin reads oauth2 clients but cannot create, overwrite or delete one`() {
        val (_, superCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc, json, userRepository, "clients-super@example.com", role = Roles.SUPER_ADMIN
        )
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc, json, userRepository, "clients-admin@example.com"
        )
        val clientId = ObjectId.get().toHexString()
        val newClientId = ObjectId.get().toHexString()
        val existing = CreateClientRequest(
            clientName = "Web App",
            clientId = clientId,
            clientSecret = "original-secret",
            redirectUris = setOf("https://example.com/callback"),
            accessTokenTtlSeconds = 900,
            refreshTokenTtlDays = 30
        )
        // The clients collection is shared with the other tests in this context, which count it.
        try {
            mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                cookie(superCookie)
                content = json.encodeToString(existing)
            }.andExpect { status { isOk() } }

            mockMvc.get("/${ApiEndpoints.OAUTH2_CLIENTS}") {
                accept = MediaType.APPLICATION_JSON
                cookie(adminCookie)
            }.andExpect { status { isOk() } }

            listOf(existing.copy(clientSecret = null), existing.copy(clientId = newClientId)).forEach { request ->
                mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
                    contentType = MediaType.APPLICATION_JSON
                    accept = MediaType.APPLICATION_JSON
                    cookie(adminCookie)
                    content = json.encodeToString(request)
                }.andExpect { status { isForbidden() } }
            }

            mockMvc.delete("/${ApiEndpoints.OAUTH2_CLIENTS}/$clientId") {
                accept = MediaType.APPLICATION_JSON
                cookie(adminCookie)
            }.andExpect { status { isForbidden() } }

            assertEquals("original-secret", clientRepository.findById(clientId).orElseThrow().clientSecret)
            assertEquals(false, clientRepository.existsById(newClientId))
        } finally {
            clientRepository.deleteAllById(listOf(clientId, newClientId))
        }
    }
}
