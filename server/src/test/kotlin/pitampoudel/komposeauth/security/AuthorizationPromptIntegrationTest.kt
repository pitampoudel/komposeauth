package pitampoudel.komposeauth.security

import jakarta.servlet.http.Cookie
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.user.repository.UserRepository
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The `prompt` handling at the authorization endpoint, exercised through the real filter chain.
 *
 * The unit tests cover the filter in isolation; what needs a running context is that it is wired
 * ahead of `OAuth2AuthorizationEndpointFilter`, which would otherwise have issued a code for the
 * existing session before the filter ever ran.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AuthorizationPromptIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    private val redirectUri = "https://rp.example.com/callback"
    private val password = "Password1"

    /**
     * Sessions live in Mongo behind a `SESSION` cookie rather than in the servlet container, so a
     * flow that spans requests is held together by carrying that cookie, not a `MockHttpSession`.
     */
    private var sessionCookie: Cookie? = null

    private fun rememberSession(result: MvcResult) {
        result.response.getCookie(SESSION_COOKIE)?.let {
            sessionCookie = if (it.maxAge == 0 || it.value.isNullOrEmpty()) null else it
        }
    }

    private fun createClient(email: String): String {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, email)
        val result = mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = json.encodeToString(
                CreateClientRequest.serializer(),
                CreateClientRequest(
                    clientName = "Prompt Test Client",
                    redirectUris = setOf(redirectUri),
                    accessTokenTtlSeconds = 900,
                    refreshTokenTtlDays = 30
                )
            )
        }.andExpect { status { isOk() } }.andReturn()

        return json.parseToJsonElement(result.response.contentAsString)
            .jsonObject["clientId"]!!.jsonPrimitive.content
    }

    /** Signs in through the login form; the resulting session is kept for the requests that follow. */
    private fun signIn(email: String) {
        val result = mockMvc.post("/session-login") {
            sessionCookie?.let { cookie(it) }
            param("username", email)
            param("password", password)
        }.andExpect { status { is3xxRedirection() } }.andReturn()

        val target = result.response.redirectedUrl
        assertTrue(target?.contains("error") != true, "sign-in failed, redirected to $target")
        rememberSession(result)
        assertNotNull(sessionCookie, "sign-in established no session")
    }

    /**
     * The parameters go in the query string rather than through `param()`: Spring Authorization
     * Server reads the authorization request out of the query string, and MockMvc's `param()`
     * leaves that empty.
     */
    private fun authorize(clientId: String, prompt: String? = null): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest("z".repeat(43).toByteArray())
        )
        // Nothing here needs percent-encoding, which is just as well: MockMvc treats the url as a
        // URI template and encodes it again, so an escape written here would arrive doubled.
        val query = buildList {
            add("response_type=code")
            add("client_id=$clientId")
            add("redirect_uri=$redirectUri")
            add("scope=openid")
            add("state=state-1")
            add("code_challenge=$challenge")
            add("code_challenge_method=S256")
            prompt?.let { add("prompt=$it") }
        }.joinToString("&")

        val result = mockMvc.get("/oauth2/authorize?$query") {
            sessionCookie?.let { cookie(it) }
            accept = MediaType.TEXT_HTML
        }.andExpect { status { is3xxRedirection() } }.andReturn()

        rememberSession(result)
        return assertNotNull(result.response.redirectedUrl)
    }

    @Test
    fun `prompt=login makes an already signed-in user log in again, and the replay then succeeds`() {
        val clientId = createClient("prompt-admin@example.com")
        val email = "prompt-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)
        signIn(email)

        // Baseline: without `prompt`, the existing session is enough to get a code.
        val withoutPrompt = authorize(clientId)
        assertTrue(withoutPrompt.startsWith("$redirectUri?code="), "was $withoutPrompt")

        // With `prompt=login`, the same session is sent back to the login page instead.
        val withPrompt = authorize(clientId, prompt = "login")
        assertTrue(withPrompt.endsWith("/session-login"), "was $withPrompt")

        // Once they have signed back in, the replayed request — same `state`, still carrying
        // `prompt` — is let through rather than bouncing to the login page forever.
        signIn(email)
        val replay = authorize(clientId, prompt = "login")
        assertTrue(replay.startsWith("$redirectUri?code="), "was $replay")
    }

    private companion object {
        const val SESSION_COOKIE = "SESSION"
    }
}
