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
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.user.repository.UserRepository
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
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

    private val redirectUri = "https://spa.example.com/auth/callback"
    private val password = "Password1"
    private val codeVerifier = "v".repeat(43)
    private var sessionCookie: Cookie? = null

    private fun createClient(
        adminEmail: String = "spa-admin@example.com",
        publicClient: Boolean = true
    ): Map<String, String> {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, adminEmail)
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
        // Reused rather than rotated (see the client's token settings), so it keeps working.
        assertEquals(refreshToken, refreshed["refresh_token"])
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
    }
}
