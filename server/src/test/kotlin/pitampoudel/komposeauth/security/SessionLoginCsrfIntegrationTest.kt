package pitampoudel.komposeauth.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.security.csrf.CrossOriginCsrfTokenRepository
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.service.UserService
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Signing in with a password, through the page a visitor is actually given.
 *
 * The token the login form renders is worth nothing without the cookie it is checked against, and
 * that cookie is written by the repository the first time anything reads the token. On this page the
 * read happens deep in the body — it carries its own stylesheet, so the hidden field lands past
 * Tomcat's 8KB response buffer — and once a response is committed `addCookie` is discarded without
 * a word. The form went out with a good token, no cookie followed it, and the POST came back 403 on
 * a whitelabel error page. Every password sign-in, every time.
 *
 * This runs against a real container on a real port, and it has to. `MockHttpServletResponse` has no
 * buffer to commit, so it accepts a cookie no matter how much has already been written — under
 * MockMvc the bug is invisible and a test built on it passes against the broken code. That was tried
 * first, and it did.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig::class)
class SessionLoginCsrfIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var userService: UserService

    /** Never follows redirects: a 302 to the home page is the result being asserted. */
    private val http: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private fun url(path: String) = URI.create("http://localhost:$port$path")

    private fun getLoginPage(): Pair<String, String> {
        val response = http.send(
            HttpRequest.newBuilder(url("/session-login")).header("Accept", "text/html").GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        val body = response.body()
        val csrfCookie = response.headers().allValues("set-cookie").firstOrNull {
            it.startsWith("${CrossOriginCsrfTokenRepository.COOKIE_NAME}=")
        }

        assertTrue(body.contains("name=\"_csrf\""), "the form rendered no token at all")
        assertNotNull(
            csrfCookie,
            "the page rendered a CSRF token but set no cookie to check it against, so the sign-in " +
                    "it leads to can only be rejected"
        )

        val token = assertNotNull(
            Regex("name=\"_csrf\" value=\"([^\"]+)\"").find(body)?.groupValues?.get(1),
            "no CSRF token value in the rendered form"
        )
        return csrfCookie.substringBefore(';') to token
    }

    @Test
    fun `the login page issues the cookie its own token is checked against`() {
        getLoginPage()
    }

    @Test
    fun `a password sign-in through the page succeeds`() {
        val email = "session-login-csrf@example.com"
        val password = "Password1"
        userService.createUser(
            baseUrl = null,
            req = CreateUserRequest(
                firstName = "Session",
                lastName = "Login",
                email = email,
                password = password,
                confirmPassword = password
            )
        )

        val (cookie, token) = getLoginPage()

        fun field(name: String, value: String) =
            "$name=" + URLEncoder.encode(value, StandardCharsets.UTF_8)

        val form = listOf(
            field("username", email),
            field("password", password),
            field("_csrf", token)
        ).joinToString("&")

        val response = http.send(
            HttpRequest.newBuilder(url("/session-login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        assertTrue(
            response.statusCode() in 300..399,
            "sign-in returned ${response.statusCode()}, not a redirect — a 403 here is the CSRF " +
                    "cookie having gone missing from the page above"
        )
    }
}
