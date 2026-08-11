package pitampoudel.komposeauth.security

import com.sun.net.httpserver.HttpServer
import jakarta.servlet.http.Cookie
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.web.util.UriComponentsBuilder
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.oauth_clients.entity.OAuth2Client
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The whole sign-in a relying party's visitor actually performs, with Google stood in for by a
 * local stub: arrive at `/oauth2/authorize` with no session, get sent to the login page, go out to
 * the provider, come back through `/login/oauth2/code/google`, and have the authorization request
 * replayed into a code.
 *
 * Every filter on that path is the real one — only the provider's HTTP endpoints and the user
 * provisioning behind [UserService.findOrCreateVerifiedGoogleUser] are stubbed, the first because
 * it needs Google's servers and the second to keep this about the sign-in path rather than the
 * account it lands on.
 */
@SpringBootTest(properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@Import(TestConfig::class, GoogleLoginReplayIntegrationTest.StubProvider::class)
@AutoConfigureMockMvc
class GoogleLoginReplayIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var oauth2ClientRepository: OAuth2ClientRepository

    @MockitoBean
    private lateinit var userService: UserService

    private val redirectUri = "https://rp.example.com/callback"

    /** The browser's cookie jar: whatever a response sets is sent back on the next request. */
    private val cookies = linkedMapOf<String, Cookie>()

    private fun rememberCookies(result: MvcResult) {
        result.response.cookies.forEach { cookie ->
            if (cookie.maxAge == 0 || cookie.value.isNullOrEmpty()) {
                cookies.remove(cookie.name)
            } else {
                cookies[cookie.name] = cookie
            }
        }
    }

    /**
     * A browser following a redirect. The URL goes in as a [URI] rather than a template, so a
     * percent-encoded `state` is sent as the provider wrote it instead of being encoded again.
     */
    private fun follow(url: String): MvcResult {
        val result = mockMvc.get(URI.create(UriComponentsBuilder.fromUriString(url).build().toUriString())) {
            cookies.values.takeIf { it.isNotEmpty() }?.let { cookie(*it.toTypedArray()) }
            accept = MediaType.TEXT_HTML
        }.andReturn()
        rememberCookies(result)
        return result
    }

    private fun registerRelyingParty(): String {
        val clientId = ObjectId.get().toHexString()
        oauth2ClientRepository.save(
            OAuth2Client(
                clientId = clientId,
                clientSecret = null,
                clientName = "Replay Test RP",
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

    private fun authorizeQuery(clientId: String): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest("z".repeat(43).toByteArray())
        )
        return listOf(
            "response_type=code",
            "client_id=$clientId",
            "redirect_uri=$redirectUri",
            "scope=openid",
            "state=rp-state",
            "code_challenge=$challenge",
            "code_challenge_method=S256"
        ).joinToString("&")
    }

    @Test
    fun `signing in through the provider replays the authorization request instead of looping to login`() {
        val clientId = registerRelyingParty()

        // The handler provisions from the claims the OIDC filter has already verified, so this is
        // the method it reaches for — nothing here re-checks the token against Google.
        whenever(userService.findOrCreateVerifiedGoogleUser(any(), any())).thenReturn(
            User(
                id = ObjectId.get(),
                firstName = "Google",
                lastName = "User",
                email = "google-user@example.com",
                emailVerified = true,
                phoneNumber = null,
                roles = listOf("USER")
            )
        )

        // 1. The relying party sends the visitor to the authorization endpoint. No session yet.
        val start = follow("/oauth2/authorize?${authorizeQuery(clientId)}")
        val toLogin = assertNotNull(
            start.response.redirectedUrl,
            "authorize returned ${start.response.status}: ${start.response.errorMessage} / ${start.response.contentAsString.take(400)}"
        )
        assertTrue(toLogin.endsWith("/session-login"), "was $toLogin")

        // 2. They pick "continue with Google", and we go out to the provider.
        val toProvider = assertNotNull(
            follow("/oauth2/authorization/google").response.redirectedUrl,
            "no redirect to the provider"
        )
        val providerParams = UriComponentsBuilder.fromUriString(toProvider).build().queryParams
        val state = assertNotNull(providerParams.getFirst("state"), "no state in $toProvider")
        // What travels on the wire is already the hash of the session-held nonce, so the ID token
        // has to echo it back verbatim.
        StubProvider.nonceHash = providerParams.getFirst("nonce")

        // 3. The provider sends them back. The stub token endpoint answers the code exchange.
        val callback = follow(
            UriComponentsBuilder.fromPath("/login/oauth2/code/google")
                .queryParam("code", "stub-code")
                .queryParam("state", URLDecoder.decode(state, StandardCharsets.UTF_8))
                .encode()
                .toUriString()
        )
        val afterLogin = assertNotNull(
            callback.response.redirectedUrl,
            "callback returned ${callback.response.status} instead of a redirect"
        )
        assertTrue(
            afterLogin.contains("/oauth2/authorize"),
            "signing in should replay the saved authorization request, but went to $afterLogin"
        )

        // 4. The replay must produce a code, not another trip to the login page.
        val replay = follow(afterLogin)
        val target = assertNotNull(replay.response.redirectedUrl, "replay returned ${replay.response.status}")
        assertTrue(target.startsWith("$redirectUri?code="), "was $target")
    }

    /**
     * A failed callback used to land on `/login?error`, where Spring Security's generated page said
     * "Invalid credentials" and offered a Google button that led straight back into the same
     * failure. Nothing about that page was this application's, and nothing about the message was
     * true.
     */
    @Test
    fun `a failed provider sign-in returns to the real login page rather than looping`() {
        registerRelyingParty()

        val callback = follow("/login/oauth2/code/google?code=stub-code&state=never-issued")

        val target = assertNotNull(
            callback.response.redirectedUrl,
            "callback returned ${callback.response.status} instead of a redirect"
        )
        assertTrue(
            target.endsWith("/session-login?error=provider"),
            "a failed provider sign-in should return to the login page with a reason, but went to $target"
        )
    }

    /**
     * A visitor who arrived from an application is in the middle of that application's sign-in.
     * When it cannot be finished, the answer belongs to the application — as an OAuth error at its
     * own redirect URI — rather than to a page of this server that the visitor never asked for.
     */
    @Test
    fun `a sign-in that fails after an application sent the visitor here goes back to it`() {
        val clientId = registerRelyingParty()

        follow("/oauth2/authorize?${authorizeQuery(clientId)}")
        val callback = follow("/login/oauth2/code/google?code=stub-code&state=never-issued")

        assertEquals(
            "$redirectUri?error=server_error&state=rp-state",
            callback.response.redirectedUrl
        )
    }

    /** Declining at the provider is not a fault, and the application should not be told it was. */
    @Test
    fun `declining at the provider is reported to the application as access_denied`() {
        val clientId = registerRelyingParty()

        follow("/oauth2/authorize?${authorizeQuery(clientId)}")
        val toProvider = assertNotNull(
            follow("/oauth2/authorization/google").response.redirectedUrl,
            "no redirect to the provider"
        )
        val state = assertNotNull(
            UriComponentsBuilder.fromUriString(toProvider).build().queryParams.getFirst("state")
        )

        val callback = follow(
            UriComponentsBuilder.fromPath("/login/oauth2/code/google")
                .queryParam("error", "access_denied")
                .queryParam("state", URLDecoder.decode(state, StandardCharsets.UTF_8))
                .encode()
                .toUriString()
        )

        assertEquals(
            "$redirectUri?error=access_denied&state=rp-state",
            callback.response.redirectedUrl
        )
    }

    /**
     * The saved authorization request lives in the session, which is the thing that goes missing in
     * every case worth recovering from. Signing in still has to end at the application: without the
     * cookie that outlives it, this landed on `/`, which answers JSON.
     */
    @Test
    fun `a sign-in whose saved request has gone still ends at the application`() {
        val clientId = registerRelyingParty()
        whenever(userService.findOrCreateVerifiedGoogleUser(any(), any())).thenReturn(
            User(
                id = ObjectId.get(),
                firstName = "Google",
                lastName = "User",
                email = "google-user@example.com",
                emailVerified = true,
                phoneNumber = null,
                roles = listOf("USER")
            )
        )

        follow("/oauth2/authorize?${authorizeQuery(clientId)}")

        // The session drops while the visitor is away at the provider, taking the saved request
        // with it. What they were doing is remembered outside it too.
        cookies.remove(SESSION_COOKIE)

        val toProvider = assertNotNull(
            follow("/oauth2/authorization/google").response.redirectedUrl,
            "no redirect to the provider"
        )
        StubProvider.nonceHash = UriComponentsBuilder.fromUriString(toProvider)
            .build().queryParams.getFirst("nonce")
        val state = assertNotNull(
            UriComponentsBuilder.fromUriString(toProvider).build().queryParams.getFirst("state")
        )

        val callback = follow(
            UriComponentsBuilder.fromPath("/login/oauth2/code/google")
                .queryParam("code", "stub-code")
                .queryParam("state", URLDecoder.decode(state, StandardCharsets.UTF_8))
                .encode()
                .toUriString()
        )
        val afterLogin = assertNotNull(
            callback.response.redirectedUrl,
            "callback returned ${callback.response.status} instead of a redirect"
        )
        assertTrue(
            afterLogin.contains("/oauth2/authorize"),
            "a signed-in visitor should be put back on the authorization request, but went to $afterLogin"
        )

        val target = assertNotNull(follow(afterLogin).response.redirectedUrl)
        assertTrue(target.startsWith("$redirectUri?code="), "was $target")
    }

    /** The other half: with the login page named, there is no generated one left to land on. */
    @Test
    fun `no second login page is served at the framework's default location`() {
        val generated = follow("/login")

        assertTrue(
            generated.response.status != 200,
            "GET /login still serves a page: ${generated.response.contentAsString.take(200)}"
        )
    }

    /**
     * Stands in for Google: an HTTP token endpoint on loopback, and a decoder that accepts the
     * opaque ID token it hands back.
     */
    @TestConfiguration(proxyBeanMethods = false)
    class StubProvider {

        @Bean
        fun clientRegistrationRepository(): ClientRegistrationRepository =
            InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId("google")
                    .clientId(PROVIDER_CLIENT_ID)
                    .clientSecret("stub-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid")
                    .authorizationUri("https://stub-provider.example.com/o/oauth2/auth")
                    .tokenUri(tokenUri)
                    .clientName("Stub Provider")
                    .build()
            )

        @Bean
        fun jwtDecoderFactory(): JwtDecoderFactory<ClientRegistration> = JwtDecoderFactory {
            JwtDecoder { token ->
                val issuedAt = Instant.now()
                Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim(IdTokenClaimNames.ISS, "https://accounts.google.com")
                    .claim(IdTokenClaimNames.SUB, "google-sub-1")
                    .claim(IdTokenClaimNames.AUD, listOf(PROVIDER_CLIENT_ID))
                    .claim(IdTokenClaimNames.AZP, PROVIDER_CLIENT_ID)
                    .claim("email", "google-user@example.com")
                    .claim("email_verified", true)
                    .apply { nonceHash?.let { claim("nonce", it) } }
                    .issuedAt(issuedAt)
                    .expiresAt(issuedAt.plusSeconds(600))
                    .build()
            }
        }

        companion object {
            const val PROVIDER_CLIENT_ID = "stub-provider-client"

            /** Set by the test once it has seen the nonce the resolver put on the outbound request. */
            @Volatile
            var nonceHash: String? = null

            private val server: HttpServer by lazy {
                HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                    createContext("/token") { exchange ->
                        val body = """
                            {"access_token":"stub-access","token_type":"Bearer","expires_in":3600,
                             "id_token":"stub-id-token","scope":"openid"}
                        """.trimIndent().replace("\n", "")
                        val bytes = body.toByteArray()
                        exchange.responseHeaders.add("Content-Type", "application/json")
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    }
                    executor = null
                    start()
                }
            }

            val tokenUri: String get() = "http://127.0.0.1:${server.address.port}/token"
        }
    }

    private companion object {
        const val SESSION_COOKIE = "SESSION"
    }
}
