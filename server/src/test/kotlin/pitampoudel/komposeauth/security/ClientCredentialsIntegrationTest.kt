package pitampoudel.komposeauth.security

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals

/**
 * A client id ships inside every app that uses it, so it proves nothing. Service tokens, which can
 * read every account, must cost the client secret.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class ClientCredentialsIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var json: Json
    @Autowired private lateinit var userRepository: UserRepository

    private val secret = "backend-secret"

    private fun createClient(): String {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "cc-admin@example.com")
        val result = mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = json.encodeToString(
                CreateClientRequest.serializer(),
                CreateClientRequest(
                    clientName = "Backend",
                    clientSecret = secret,
                    accessTokenTtlSeconds = 900,
                    refreshTokenTtlDays = 30,
                    scopes = setOf("user.read.any")
                )
            )
        }.andExpect { status { isOk() } }.andReturn()
        return json.parseToJsonElement(result.response.contentAsString).jsonObject["clientId"]!!.jsonPrimitive.content
    }

    private fun tokenStatus(vararg params: Pair<String, String>) = mockMvc.post("/oauth2/token") {
        // As a backend's HTTP client sends it; with no Accept at all the server treats the caller
        // as a browser and redirects to the sign-in page instead.
        accept = MediaType.APPLICATION_JSON
        param("grant_type", "client_credentials")
        param("scope", "user.read.any")
        params.forEach { (name, value) -> param(name, value) }
    }.andReturn().response

    @Test
    fun `client credentials need the secret`() {
        val clientId = createClient()

        assertEquals(401, tokenStatus("client_id" to clientId).status)
        assertEquals(401, tokenStatus("client_id" to clientId, "client_secret" to "wrong").status)

        val response = tokenStatus("client_id" to clientId, "client_secret" to secret)
        assertEquals(200, response.status, response.contentAsString)
        val accessToken = json.parseToJsonElement(response.contentAsString)
            .jsonObject["access_token"]!!.jsonPrimitive.content

        mockMvc.get("/${ApiEndpoints.USERS}") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect { status { isOk() } }
    }
}
