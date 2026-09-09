package pitampoudel.komposeauth.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.oauth_clients.entity.OAuth2Client
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.service.UserService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The sign-in driven over real HTTP against a real servlet container, rather than through MockMvc.
 *
 * MockMvc dispatches straight into the filter chain: there is no container managing the session,
 * and the cookies a test carries between requests are whatever the test chose to copy across. That
 * is enough to cover routing and redirects, and it is what the other tests here do -- but it cannot
 * show what happens to an authenticated session when the container rotates its id on sign-in, which
 * is the one thing a browser experiences and a MockMvc test does not.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig::class)
class RealContainerLoginIntegrationTest {

    @LocalServerPort private var port: Int = 0
    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var oauth2ClientRepository: OAuth2ClientRepository

    private val password = "Password1"
    private val codeVerifier = "z".repeat(43)
    private lateinit var redirectUri: String

    private fun base() = "http://localhost:$port"

    private fun registerClient(): String {
        val clientId = ObjectId.get().toHexString()
        oauth2ClientRepository.save(
            OAuth2Client(
                clientId = clientId,
                clientSecret = null,
                clientName = "Real Container Client",
                clientAuthenticationMethods = setOf(ClientAuthenticationMethod.NONE),
                authorizationGrantTypes = setOf(AuthorizationGrantType.AUTHORIZATION_CODE),
                redirectUris = setOf(redirectUri),
                scopes = setOf("openid"),
                requireAuthorizationConsent = false,
                clientUri = null,
                logoUri = null
            )
        )
        return clientId
    }

    private fun authorizeUrl(clientId: String, prompt: String? = "login"): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        )
        fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)
        return base() + "/oauth2/authorize?response_type=code&client_id=$clientId" +
                "&redirect_uri=${enc(redirectUri)}&scope=openid&state=state-1" +
                "&code_challenge=$challenge&code_challenge_method=S256" +
                (prompt?.let { "&prompt=$it" } ?: "")
    }

    /**
     * Signing in at the login page directly, with no authorization request waiting -- a stale tab, a
     * bookmarked login page, a visitor who arrived at `/session-login` any way other than through
     * `/oauth2/authorize`.
     *
     * There is nowhere in particular to send them afterwards, and `SavedRequestAwareAuthenticationSuccessHandler`
     * falls back to `/`. What must not happen is what did: `/` bounced an authenticated non-admin
     * straight back to `/session-login`, so a correct password re-rendered the login page, and
     * submitting it again did the same thing. No error was ever shown, because nothing failed.
     */
    @Test
    fun `signing in with nothing waiting does not land back on the login page`() {
        redirectUri = "http://localhost:$port/rp-callback"
        registerClient()
        val email = "no-saved-request@example.com"
        userService.createUser(
            null,
            CreateUserRequest(
                firstName = "Direct",
                lastName = "User",
                email = email,
                password = password,
                confirmPassword = password
            )
        )

        val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        val http = HttpClient.newBuilder()
            .cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

        fun get(url: String): HttpResponse<String> = http.send(
            HttpRequest.newBuilder(URI.create(url)).header("Accept", "text/html").GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        // Straight to the login page: nothing saved the way an authorization request would have.
        get(base() + "/session-login")

        fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)
        val login = http.send(
            HttpRequest.newBuilder(URI.create(base() + "/session-login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "username=${enc(email)}&password=${enc(password)}"
                ))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(302, login.statusCode())
        val afterLogin = login.headers().firstValue("location").orElse(null)
        assertNotNull(afterLogin)
        assertTrue(afterLogin.contains("error") != true, "the sign-in was rejected: $afterLogin")

        // Follow where it sends them. Landing on the login page again is the loop.
        val trail = mutableListOf("POST /session-login -> 302 $afterLogin")
        var url: String? = if (afterLogin.startsWith("http")) afterLogin else base() + afterLogin
        var hops = 0
        while (url != null && hops++ < 5) {
            if (!url.startsWith(base())) break // left this server entirely, which is fine
            val response = get(url)
            val location = response.headers().firstValue("location").orElse(null)
            trail += "GET ${url.removePrefix(base())} -> ${response.statusCode()} ${location ?: "(page)"}"
            assertTrue(
                location?.contains("/session-login") != true,
                "a successful sign-in was sent back to the login page: $trail"
            )
            if (location == null) break
            url = if (location.startsWith("http")) location else base() + location
        }
        println("=== AFTER SIGN-IN TRAIL ===" + trail.joinToString(System.lineSeparator(), prefix = System.lineSeparator()))
    }

    /**
     * The same sign-in, with one detour: the visitor opens the root — a second tab, a bookmark, a
     * link back to this host — after being sent to the login page and before filling it in.
     *
     * That is enough to lose the relying party. The saved request is a single slot in the session,
     * and every unauthenticated request overwrites it, so the root replaces the authorization
     * request with itself; the sign-in then goes to the root, which said "You're signed in — head
     * back to the app you came from". Nothing had gone wrong that a visitor could see, and nothing
     * had gone right: the relying party was never told, and going back to it starts the whole
     * sign-in again.
     *
     * Only the arriving-of-their-own-accord case is allowed to end on that page, and
     * `a browser at the root gets the landing page` is where that is held down.
     */
    @Test
    fun `a detour to the root does not lose the relying party`() {
        redirectUri = "http://localhost:$port/rp-callback"
        val clientId = registerClient()
        val email = "detour-to-root@example.com"
        userService.createUser(
            null,
            CreateUserRequest(
                firstName = "Detoured",
                lastName = "User",
                email = email,
                password = password,
                confirmPassword = password
            )
        )

        val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        val http = HttpClient.newBuilder()
            .cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

        fun get(url: String): HttpResponse<String> = http.send(
            HttpRequest.newBuilder(URI.create(url)).header("Accept", "text/html").GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        val trail = mutableListOf<String>()
        fun record(what: String, response: HttpResponse<String>): String? {
            val location = response.headers().firstValue("location").orElse(null)
            trail += "$what -> ${response.statusCode()} ${location ?: "(page)"}"
            assertTrue(
                !response.body().contains("You're signed in"),
                "the sign-in ended on the landing page instead of at the relying party: $trail"
            )
            return location
        }

        // 1. The relying party sends them to the authorization endpoint, and on to the login page.
        val toLogin = record("GET /oauth2/authorize", get(authorizeUrl(clientId, prompt = null)))
        assertNotNull(toLogin, "the authorization request did not send an anonymous visitor anywhere")
        assertTrue(toLogin.contains("/session-login"), "was $toLogin")

        // 2. The detour. This is the request that overwrites the authorization request.
        record("GET /", get(base() + "/"))

        // 3. Back to the login page, and in.
        get(base() + "/session-login")

        fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)
        val login = http.send(
            HttpRequest.newBuilder(URI.create(base() + "/session-login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=${enc(email)}&password=${enc(password)}"
                    )
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        var location = record("POST /session-login", login)
        assertTrue(location?.contains("error") != true, "the sign-in was rejected: $trail")

        // 4. Wherever it leads, it has to end at the relying party with a code.
        var callback: String? = null
        var hops = 0
        while (location != null && hops++ < 6) {
            if (location.startsWith(redirectUri)) {
                callback = location
                break
            }
            val url = if (location.startsWith("http")) location else base() + location
            assertTrue(
                !url.contains("/session-login"),
                "a completed sign-in was sent back to the login page: $trail"
            )
            location = record("GET ${url.removePrefix(base())}", get(url))
        }

        println("=== DETOUR TRAIL ===" + trail.joinToString(System.lineSeparator(), prefix = System.lineSeparator()))
        assertNotNull(callback, "the sign-in never reached the relying party: $trail")
        assertTrue(callback.contains("?code="), "was $callback")
    }

    @Test
    fun `a password sign-in survives the session id rotation and reaches the relying party`() {
        redirectUri = "http://localhost:$port/rp-callback"
        val clientId = registerClient()
        val email = "real-container@example.com"
        userService.createUser(
            null, // no base url: skips the welcome mail, which has no server to reach in tests
            CreateUserRequest(
                firstName = "Real",
                lastName = "User",
                email = email,
                password = password,
                confirmPassword = password
            )
        )

        // A real cookie jar, so the session cookie is handled the way a browser handles it.
        val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        val http = HttpClient.newBuilder()
            .cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

        fun get(url: String): HttpResponse<String> = http.send(
            HttpRequest.newBuilder(URI.create(url)).header("Accept", "text/html").GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        fun postLogin(): HttpResponse<String> {
            fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)
            val form = "username=${enc(email)}&password=${enc(password)}"
            return http.send(
                HttpRequest.newBuilder(URI.create(base() + "/session-login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "text/html")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
        }

        var url: String? = authorizeUrl(clientId)
        var signIns = 0
        var callback: String? = null
        val trail = mutableListOf<String>()
        var hops = 0

        while (url != null && hops++ < 12) {
            val response = get(url)
            val location = response.headers().firstValue("location").orElse(null)
            trail += "GET ${url.removePrefix(base())} -> ${response.statusCode()} ${location ?: "(page)"}"

            if (location != null && location.startsWith(redirectUri)) {
                callback = location
                break
            }
            if (location != null) {
                url = if (location.startsWith("http")) location else base() + location
                continue
            }

            assertEquals(200, response.statusCode(), "unexpected page: $trail")

            signIns++
            val login = postLogin()
            val next = login.headers().firstValue("location").orElse(null)
            trail += "POST /session-login -> ${login.statusCode()} $next"
            assertTrue(
                next?.contains("error") != true,
                "the sign-in was rejected, so this test is not testing what it means to: $trail"
            )
            url = next?.let { if (it.startsWith("http")) it else base() + it }
        }

        println("=== REAL CONTAINER TRAIL ===\n" + trail.joinToString("\n"))
        assertNotNull(callback, "the sign-in never reached the relying party: $trail")
        assertTrue(callback.contains("?code="), "was $callback")
        assertEquals(1, signIns, "the visitor was asked to sign in $signIns times: $trail")
    }
}
