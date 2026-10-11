package pitampoudel.komposeauth.security

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
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
import pitampoudel.komposeauth.authorization.MongoOAuth2AuthorizationService
import pitampoudel.komposeauth.authorization.OAuth2AuthorizationDocumentRepository
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.user.data.LogoutRequest
import pitampoudel.komposeauth.user.repository.UserRepository
import java.net.URI
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A browser app is a public client: it signs in with the authorization code flow and PKCE and
 * holds no client secret. It still needs a refresh token, or its users are sent back through the
 * sign-in redirect every time the access token expires.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class PublicClientRefreshTokenIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var json: Json
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var authorizations: OAuth2AuthorizationDocumentRepository

    private companion object {
        const val CONTENDERS = 6
    }

    private val redirectUri = "https://spa.example.com/auth/callback"
    private val password = "Password1"
    private val codeVerifier = "v".repeat(43)
    private var sessionCookie: Cookie? = null

    private fun createClient(
        adminEmail: String = "spa-admin@example.com",
        publicClient: Boolean = true
    ): Map<String, String> {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, adminEmail, role = Roles.SUPER_ADMIN)
        val result = mockMvc.post("/${ApiEndpoints.OAUTH2_CLIENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = json.encodeToString(
                CreateClientRequest.serializer(),
                CreateClientRequest(
                    clientName = "Browser App",
                    publicClient = publicClient,
                    redirectUris = setOf(redirectUri),
                    accessTokenTtlSeconds = 900,
                    refreshTokenTtlDays = 30,
                    scopes = setOf("openid", "profile", "email", "user.read.any")
                )
            )
        }.andExpect { status { isOk() } }.andReturn()
        return json.parseToJsonElement(result.response.contentAsString).jsonObject
            .filterValues { it is JsonPrimitive && it.isString }
            .mapValues { it.value.jsonPrimitive.content }
    }

    private fun remember(result: MvcResult) {
        result.response.getCookie("SESSION")?.let {
            sessionCookie = if (it.maxAge == 0 || it.value.isNullOrEmpty()) null else it
        }
    }

    /** Signs in through the hosted login page and returns the authorization code. */
    private fun authorize(clientId: String, email: String): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        )
        var url: String? = "/oauth2/authorize?response_type=code&client_id=$clientId&redirect_uri=$redirectUri" +
            "&scope=openid%20user.read.any&state=s1&code_challenge=$challenge&code_challenge_method=S256"
        var hops = 0
        while (url != null && hops++ < 12) {
            // As a URI, so the already-encoded query (the space in `scope`) is not encoded again.
            val result = mockMvc.get(URI(url)) {
                sessionCookie?.let { cookie(it) }
                accept = MediaType.TEXT_HTML
            }.andReturn()
            remember(result)
            val location = result.response.redirectedUrl
            if (location != null && location.startsWith(redirectUri)) {
                check(location.startsWith("$redirectUri?code=")) { "sign-in failed: $location" }
                return location.substringAfter("?code=").substringBefore("&")
            }
            url = location ?: mockMvc.post("/session-login") {
                sessionCookie?.let { cookie(it) }
                param("username", email)
                param("password", password)
            }.andReturn().also { remember(it) }.response.redirectedUrl
        }
        error("the sign-in never reached the app")
    }

    private fun tokenRequest(vararg params: Pair<String, String>): MvcResult = mockMvc.post("/oauth2/token") {
        accept = MediaType.APPLICATION_JSON
        params.forEach { (name, value) -> param(name, value) }
    }.andReturn()

    private fun token(vararg params: Pair<String, String>): Map<String, String> {
        val result = tokenRequest(*params)
        assertEquals(200, result.response.status, "${params.first().second}: ${result.response.contentAsString}")
        return json.parseToJsonElement(result.response.contentAsString).jsonObject
            .mapValues { it.value.jsonPrimitive.content }
    }

    @Test
    fun `a public client gets a refresh token and can use it without a secret`() {
        val clientId = createClient().getValue("clientId")
        assertNull(createClient("spa-admin-3@example.com")["clientSecret"], "a public client was given a secret")
        val email = "spa-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        val first = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to codeVerifier
        )
        val refreshToken = assertNotNull(first["refresh_token"], "no refresh token in $first")
        // Reaching every account is for a client acting as itself, never for a signed-in user.
        val claims = json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(first.getValue("access_token").split(".")[1]))
        ).jsonObject
        assertFalse("user.read.any" in claims["scope"].toString(), "user token carries a service scope: $claims")

        val refreshed = token(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to clientId
        )
        assertNotNull(refreshed["access_token"])
        assertNotEquals(first["access_token"], refreshed["access_token"])
        // Rotated: the refresh hands back a new refresh token, which is the one that works from now on.
        val rotated = assertNotNull(refreshed["refresh_token"])
        assertNotEquals(refreshToken, rotated)
        assertNotNull(token("grant_type" to "refresh_token", "refresh_token" to rotated, "client_id" to clientId)["access_token"])
    }

    private fun signIn(adminEmail: String, email: String): Pair<String, String> {
        val clientId = createClient(adminEmail).getValue("clientId")
        TestAuthHelpers.createUser(mockMvc, json, email, password)
        val refreshToken = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to codeVerifier
        ).getValue("refresh_token")
        return clientId to refreshToken
    }

    private fun refresh(clientId: String, refreshToken: String) = tokenRequest(
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
        "client_id" to clientId
    )

    private fun refreshedToken(result: MvcResult): String {
        assertEquals(200, result.response.status, result.response.contentAsString)
        return json.parseToJsonElement(result.response.contentAsString).jsonObject.getValue("refresh_token").jsonPrimitive.content
    }

    private fun assertRefused(result: MvcResult) {
        assertEquals(400, result.response.status, result.response.contentAsString)
        assertTrue("invalid_grant" in result.response.contentAsString, result.response.contentAsString)
    }

    @Test
    fun `a refresh made again with the token it replaced gets the same new token`() {
        val (clientId, first) = signIn("spa-admin-7@example.com", "spa-rotation@example.com")

        val second = refreshedToken(refresh(clientId, first))
        // What a second request of the same page load does: it read the cookie before the first
        // request's answer replaced it.
        assertEquals(second, refreshedToken(refresh(clientId, first)))
        assertNotEquals(second, refreshedToken(refresh(clientId, second)))
    }

    @Test
    fun `concurrent refreshes with one token all end up holding the same token`() {
        val (clientId, first) = signIn("spa-admin-8@example.com", "spa-concurrent@example.com")

        val pool = Executors.newFixedThreadPool(CONTENDERS)
        val start = CountDownLatch(1)
        val answers = try {
            (1..CONTENDERS).map {
                pool.submit<MvcResult> {
                    start.await()
                    refresh(clientId, first)
                }
            }.also { start.countDown() }.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        val held = answers.map(::refreshedToken).toSet()
        assertEquals(1, held.size, "the requests came away with ${held.size} different refresh tokens")
        assertNotEquals(first, held.single())
        refreshedToken(refresh(clientId, held.single()))
    }

    @Test
    fun `a replaced refresh token used after the grace ends the authorization`() {
        val (clientId, first) = signIn("spa-admin-9@example.com", "spa-replay@example.com")
        val second = refreshedToken(refresh(clientId, first))
        val stored = assertNotNull(authorizations.findByRefreshTokenValue(second))
        authorizations.save(
            stored.copy(refreshTokenRotatedAt = Instant.now() - MongoOAuth2AuthorizationService.ROTATION_GRACE - Duration.ofSeconds(1))
        )

        assertRefused(refresh(clientId, first))
        // Whoever was holding the newer token is out too: either of them may be the one who stole it.
        assertRefused(refresh(clientId, second))
        assertNull(authorizations.findById(stored.id).orElse(null))
    }

    @Test
    fun `a registered client's token carries the roles, and a deactivated account cannot refresh`() {
        val clientId = createClient("spa-admin-2@example.com").getValue("clientId")
        val email = "spa-deactivated@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        val first = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to codeVerifier
        )
        val claims = json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(first.getValue("access_token").split(".")[1]))
        ).jsonObject
        assertTrue("authorities" in claims, "no authorities in $claims")
        // The ID token is the one passed around (id_token_hint, logs), so it carries no roles.
        val idTokenClaims = json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(first.getValue("id_token").split(".")[1]))
        ).jsonObject
        assertFalse("authorities" in idTokenClaims, "the ID token carries roles: $idTokenClaims")

        // Closed behind the authorization's back, as an account deactivated before its
        // authorizations were revoked on deactivation would be.
        val user = assertNotNull(userRepository.findByEmail(email))
        userRepository.save(user.copy(deactivated = true))

        val refused = mockMvc.post("/oauth2/token") {
            param("grant_type", "refresh_token")
            param("refresh_token", first.getValue("refresh_token"))
            param("client_id", clientId)
        }.andReturn()
        assertEquals(400, refused.response.status, refused.response.contentAsString)
        assertTrue("invalid_grant" in refused.response.contentAsString, refused.response.contentAsString)
    }

    @Test
    fun `a code is not redeemed without its PKCE verifier`() {
        val clientId = createClient("spa-admin-4@example.com").getValue("clientId")
        val email = "spa-pkce@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        // With no verifier and no secret nothing authenticates the client, so it is a 401, not a 400.
        val missing = tokenRequest(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId
        )
        assertEquals(401, missing.response.status, missing.response.contentAsString)

        val wrong = tokenRequest(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to "w".repeat(43)
        )
        assertEquals(400, wrong.response.status, wrong.response.contentAsString)
        assertTrue("invalid_grant" in wrong.response.contentAsString, wrong.response.contentAsString)
    }

    @Test
    fun `a confidential client cannot refresh with its client id alone`() {
        val client = createClient("spa-admin-5@example.com", publicClient = false)
        val clientId = client.getValue("clientId")
        val secret = client.getValue("clientSecret")
        val email = "spa-confidential@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        val first = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "client_secret" to secret,
            "code_verifier" to codeVerifier
        )
        val refreshToken = first.getValue("refresh_token")
        // A registered app's token is a bearer on this server's own API
        val me = mockMvc.get("/${ApiEndpoints.ME}") { header("Authorization", "Bearer ${first.getValue("access_token")}") }.andReturn()
        assertEquals(200, me.response.status, me.response.contentAsString)

        val bare = tokenRequest(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to clientId
        )
        assertEquals(401, bare.response.status, bare.response.contentAsString)

        val withSecret = token(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to clientId,
            "client_secret" to secret
        )
        assertNotNull(withSecret["access_token"])
        // Rotated for a confidential client too, and the new token is the one that refreshes.
        val rotated = withSecret.getValue("refresh_token")
        assertNotEquals(refreshToken, rotated)
        val again = token(
            "grant_type" to "refresh_token",
            "refresh_token" to rotated,
            "client_id" to clientId,
            "client_secret" to secret
        )
        assertNotNull(again["access_token"])
    }

    private data class ConfidentialSignIn(val clientId: String, val secret: String, val refreshToken: String)

    private fun signInConfidential(adminEmail: String, email: String): ConfidentialSignIn {
        val client = createClient(adminEmail, publicClient = false)
        val clientId = client.getValue("clientId")
        val secret = client.getValue("clientSecret")
        TestAuthHelpers.createUser(mockMvc, json, email, password)
        val refreshToken = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "client_secret" to secret,
            "code_verifier" to codeVerifier
        ).getValue("refresh_token")
        return ConfidentialSignIn(clientId, secret, refreshToken)
    }

    /** What a server-side app sends when its user signs out (RFC 7009, `client_secret_post`). */
    private fun revoke(clientId: String, secret: String, token: String): MvcResult = mockMvc.post("/oauth2/revoke") {
        param("token", token)
        param("token_type_hint", "refresh_token")
        param("client_id", clientId)
        param("client_secret", secret)
    }.andReturn()

    private fun refreshWithSecret(signIn: ConfidentialSignIn, refreshToken: String) = tokenRequest(
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
        "client_id" to signIn.clientId,
        "client_secret" to signIn.secret
    )

    @Test
    fun `a confidential client revokes a refresh token with its secret in the form, and it stops working`() {
        val signIn = signInConfidential("spa-admin-10@example.com", "spa-revoke@example.com")

        assertEquals(401, revoke(signIn.clientId, "not-the-secret", signIn.refreshToken).response.status)
        val revoked = revoke(signIn.clientId, signIn.secret, signIn.refreshToken)
        assertEquals(200, revoked.response.status, revoked.response.contentAsString)
        assertEquals("", revoked.response.contentAsString)

        assertRefused(refreshWithSecret(signIn, signIn.refreshToken))
        // Revoking again, or revoking anything unknown, is not an error.
        assertEquals(200, revoke(signIn.clientId, signIn.secret, signIn.refreshToken).response.status)
        assertEquals(200, revoke(signIn.clientId, signIn.secret, "never-issued").response.status)
    }

    @Test
    fun `revoking the refresh token a refresh just replaced signs out the one that replaced it`() {
        val signIn = signInConfidential("spa-admin-11@example.com", "spa-revoke-retired@example.com")
        val replacement = refreshedToken(refreshWithSecret(signIn, signIn.refreshToken))
        val authorizationId = assertNotNull(authorizations.findByRefreshTokenValue(replacement)).id

        // A sign-out reading the cookie the request arrived with, before the refresh replaced it.
        assertEquals(200, revoke(signIn.clientId, signIn.secret, signIn.refreshToken).response.status)

        assertRefused(refreshWithSecret(signIn, replacement))
        assertRefused(refreshWithSecret(signIn, signIn.refreshToken))
        assertNull(authorizations.findById(authorizationId).orElse(null))
    }

    @Test
    fun `a client cannot revoke another client's replaced refresh token`() {
        val signIn = signInConfidential("spa-admin-12@example.com", "spa-revoke-owner@example.com")
        val replacement = refreshedToken(refreshWithSecret(signIn, signIn.refreshToken))
        val other = createClient("spa-admin-13@example.com", publicClient = false)

        // Answered as for any token it never issued.
        assertEquals(200, revoke(other.getValue("clientId"), other.getValue("clientSecret"), signIn.refreshToken).response.status)

        assertNotNull(refreshedToken(refreshWithSecret(signIn, replacement)))
    }

    @Test
    fun `a public client, which cannot authenticate to the revocation endpoint, signs out at logout`() {
        val (clientId, refreshToken) = signIn("spa-admin-14@example.com", "spa-logout@example.com")

        val result = mockMvc.post("/${ApiEndpoints.LOGOUT}") {
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(LogoutRequest(refreshToken = refreshToken))
        }.andReturn()
        assertEquals(200, result.response.status, result.response.contentAsString)

        assertRefused(refresh(clientId, refreshToken))
    }

    private fun claimsOf(jwt: String) = json.parseToJsonElement(
        Base64.getUrlDecoder().decode(jwt.split(".")[1]).decodeToString()
    ).jsonObject

    @Test
    fun `an access token says when the user signed in, and a refreshed one keeps saying it`() {
        val clientId = createClient("spa-admin-15@example.com").getValue("clientId")
        val email = "spa-auth-time@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val before = Instant.now().epochSecond

        val first = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to codeVerifier
        )
        val signedInAt = assertNotNull(claimsOf(first.getValue("access_token"))["auth_time"]).jsonPrimitive.content.toLong()
        assertTrue(signedInAt in before..Instant.now().epochSecond, "auth_time $signedInAt")

        val refreshed = token("grant_type" to "refresh_token", "refresh_token" to first.getValue("refresh_token"), "client_id" to clientId)
        assertEquals(signedInAt, claimsOf(refreshed.getValue("access_token"))["auth_time"]?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun `userinfo answers for the granted scopes only, and refuses a token whose user is gone`() {
        val clientId = createClient("spa-admin-6@example.com").getValue("clientId")
        val email = "spa-userinfo@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)
        val accessToken = token(
            "grant_type" to "authorization_code",
            "code" to authorize(clientId, email),
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to codeVerifier
        ).getValue("access_token")

        fun userInfo() = mockMvc.get("/userinfo") { header("Authorization", "Bearer $accessToken") }.andReturn().response

        // Signed in with `openid` alone: who it is, and, for a client registered here, the roles
        val info = json.parseToJsonElement(userInfo().contentAsString).jsonObject
        assertTrue("sub" in info && "roles" in info, "$info")
        assertFalse("email" in info || "givenName" in info || "phoneNumberVerified" in info, "$info")

        userRepository.deleteById(assertNotNull(userRepository.findByEmail(email)).id)
        assertEquals(401, userInfo().status)
    }
}
