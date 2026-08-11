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
 * The path a relying party's visitor actually takes: arrive at `/oauth2/authorize` with no session,
 * get sent to the login page, sign in, and have the authorization request replayed.
 *
 * [AuthorizationPromptIntegrationTest] only ever authorizes from an already-signed-in session, so
 * the save-and-replay leg — the one every first-time sign-in goes through — was never exercised.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AuthorizationLoginReplayIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    private val redirectUri = "https://rp.example.com/callback"
    private val password = "Password1"

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
                    clientName = "Replay Test Client",
                    redirectUris = setOf(redirectUri),
                    accessTokenTtlSeconds = 900,
                    refreshTokenTtlDays = 30
                )
            )
        }.andExpect { status { isOk() } }.andReturn()

        return json.parseToJsonElement(result.response.contentAsString)
            .jsonObject["clientId"]!!.jsonPrimitive.content
    }

    private fun authorizeQuery(clientId: String): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest("z".repeat(43).toByteArray())
        )
        return listOf(
            "response_type=code",
            "client_id=$clientId",
            "redirect_uri=$redirectUri",
            "scope=openid",
            "state=state-1",
            "code_challenge=$challenge",
            "code_challenge_method=S256"
        ).joinToString("&")
    }

    /** A browser following a redirect: GET the location, carrying whatever session we hold. */
    private fun follow(url: String): MvcResult {
        val result = mockMvc.get(url) {
            sessionCookie?.let { cookie(it) }
            accept = MediaType.TEXT_HTML
        }.andReturn()
        rememberSession(result)
        return result
    }

    @Test
    fun `a visitor with no session is sent to log in and the authorization request is replayed`() {
        val clientId = createClient("replay-admin@example.com")
        val email = "replay-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        // 1. The relying party sends the visitor to the authorization endpoint. No session yet.
        val first = follow("/oauth2/authorize?${authorizeQuery(clientId)}")
        val toLogin = assertNotNull(first.response.redirectedUrl, "expected a redirect to the login page")
        assertTrue(toLogin.endsWith("/session-login"), "was $toLogin")
        assertNotNull(sessionCookie, "no session was established to hold the saved request")

        // 2. The login page renders.
        follow("/session-login").also {
            assertTrue(it.response.status == 200, "login page returned ${it.response.status}")
        }

        // 3. They sign in.
        val login = mockMvc.post("/session-login") {
            sessionCookie?.let { cookie(it) }
            param("username", email)
            param("password", password)
        }.andExpect { status { is3xxRedirection() } }.andReturn()
        rememberSession(login)

        val afterLogin = assertNotNull(login.response.redirectedUrl)
        assertTrue(
            afterLogin.contains("/oauth2/authorize"),
            "sign-in should replay the saved authorization request, but went to $afterLogin"
        )

        // 4. The replay must produce a code, not another trip to the login page.
        val replay = follow(afterLogin)
        val target = assertNotNull(replay.response.redirectedUrl, "replay returned ${replay.response.status}")
        assertTrue(target.startsWith("$redirectUri?code="), "was $target")
    }

    private companion object {
        const val SESSION_COOKIE = "SESSION"
    }
}
