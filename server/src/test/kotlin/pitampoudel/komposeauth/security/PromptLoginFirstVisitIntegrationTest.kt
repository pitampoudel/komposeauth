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
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.user.repository.UserRepository
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A first-time visitor arriving with `prompt=login`, which is what a relying party sends when it
 * wants the account chooser rather than whoever this server last signed in.
 *
 * [AuthorizationPromptIntegrationTest] starts from a session that is already signed in, and
 * [AuthorizationLoginReplayIntegrationTest] starts from none but sends no `prompt` — so the case
 * every first sign-in from that relying party actually takes, no session *and* `prompt=login`, was
 * covered by neither. It cost the visitor two sign-ins: the authorization request that came back
 * after the first one had never been recorded as having been through the login page, so it was
 * treated as a fresh demand for one and sent them straight back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class PromptLoginFirstVisitIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var json: Json
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var appConfigProvider: AppConfigProvider

    private val redirectUri = "https://rp.example.com/callback"
    private val password = "Password1"
    private val codeVerifier = "z".repeat(43)

    private var sessionCookie: Cookie? = null
    private lateinit var clientSecret: String

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
                    clientName = "First Visit Client",
                    redirectUris = setOf(redirectUri),
                    accessTokenTtlSeconds = 900,
                    refreshTokenTtlDays = 30
                )
            )
        }.andExpect { status { isOk() } }.andReturn()

        val body = json.parseToJsonElement(result.response.contentAsString).jsonObject
        clientSecret = body["clientSecret"]!!.jsonPrimitive.content
        return body["clientId"]!!.jsonPrimitive.content
    }

    private fun authorizeUrl(clientId: String): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        )
        val query = listOf(
            "response_type=code",
            "client_id=$clientId",
            "redirect_uri=$redirectUri",
            "scope=openid",
            "state=state-1",
            "code_challenge=$challenge",
            "code_challenge_method=S256",
            "prompt=login"
        ).joinToString("&")
        return "/oauth2/authorize?$query"
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

    private fun submitLogin(email: String): MvcResult {
        val result = mockMvc.post("/session-login") {
            sessionCookie?.let { cookie(it) }
            param("username", email)
            param("password", password)
        }.andReturn()
        rememberSession(result)
        return result
    }

    @Test
    fun `a first sign-in with prompt=login is asked for once and the code it returns is redeemable`() {
        val clientId = createClient("first-visit-admin@example.com")
        val email = "first-visit-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        var url: String? = authorizeUrl(clientId)
        var signIns = 0
        var callback: String? = null
        val trail = mutableListOf<String>()

        // Bounded, so a flow that never settles fails as a loop rather than hanging the suite.
        var hops = 0
        while (url != null && hops++ < MAX_HOPS) {
            val result = follow(url)
            val location = result.response.redirectedUrl
            trail += "GET $url -> ${result.response.status} ${location ?: "(page)"}"

            if (location != null && location.startsWith(redirectUri)) {
                callback = location
                break
            }
            if (location != null) {
                url = location
                continue
            }

            // The login page rendered, so somebody has to sign in.
            assertEquals(200, result.response.status, "unexpected page in the sign-in flow: $trail")
            signIns++
            val login = submitLogin(email)
            trail += "POST /session-login -> ${login.response.status} ${login.response.redirectedUrl}"
            url = login.response.redirectedUrl
        }

        assertNotNull(callback, "the sign-in never reached the relying party: $trail")
        assertTrue(callback.contains("?code="), "was $callback")
        assertEquals(1, signIns, "the visitor was asked to sign in $signIns times: $trail")

        // The code has to be redeemable: a password sign-in stores a different principal in the
        // authorization than a provider sign-in does, and only the token exchange reads it back.
        val code = callback.substringAfter("?code=").substringBefore("&")
        mockMvc.post("/oauth2/token") {
            param("grant_type", "authorization_code")
            param("code", code)
            param("redirect_uri", redirectUri)
            param("client_id", clientId)
            param("client_secret", clientSecret)
            param("code_verifier", codeVerifier)
        }.andExpect {
            status { isOk() }
            jsonPath("$.access_token") { exists() }
            jsonPath("$.id_token") { exists() }
        }
    }

    /**
     * The same flow with Google configured, which is how every real deployment runs and how the
     * previous test does *not*: with no provider credentials, `googleEnabled` is false and the
     * login page never consults the pending authorization request at all. Configured, it does —
     * and an `idp` it does not find must leave the password form standing rather than handing the
     * visitor to the provider.
     */
    @Test
    fun `a password sign-in still completes when Google is configured`() {
        appConfigProvider.save(
            AppConfig(
                googleAuthClientId = "test-google-client-id",
                googleAuthClientSecret = "test-google-client-secret"
            )
        )
        try {
            val clientId = createClient("google-on-admin@example.com")
            val email = "google-on-user@example.com"
            TestAuthHelpers.createUser(mockMvc, json, email, password)

            var url: String? = authorizeUrl(clientId)
            var signIns = 0
            var callback: String? = null
            val trail = mutableListOf<String>()

            var hops = 0
            while (url != null && hops++ < MAX_HOPS) {
                val result = follow(url)
                val location = result.response.redirectedUrl
                trail += "GET $url -> ${result.response.status} ${location ?: "(page)"}"

                assertTrue(
                    location?.contains("/oauth2/authorization/google") != true,
                    "a password sign-in was handed to the provider: $trail"
                )

                if (location != null && location.startsWith(redirectUri)) {
                    callback = location
                    break
                }
                if (location != null) {
                    url = location
                    continue
                }

                assertEquals(200, result.response.status, "unexpected page in the sign-in flow: $trail")
                signIns++
                val login = submitLogin(email)
                trail += "POST /session-login -> ${login.response.status} ${login.response.redirectedUrl}"
                url = login.response.redirectedUrl
            }

            assertNotNull(callback, "the sign-in never reached the relying party: $trail")
            assertTrue(callback.contains("?code="), "was $callback")
            assertEquals(1, signIns, "the visitor was asked to sign in $signIns times: $trail")
        } finally {
            // Shared context: leave the configuration as it was found.
            appConfigProvider.save(AppConfig())
        }
    }

    private companion object {
        const val SESSION_COOKIE = "SESSION"
        const val MAX_HOPS = 12
    }
}
