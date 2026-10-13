package pitampoudel.komposeauth.webauthn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import jakarta.servlet.http.Cookie
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.webauthn.entity.WebAuthnChallenge
import pitampoudel.komposeauth.webauthn.repository.PublicKeyCredentialRepository
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Passkeys end to end against a software authenticator: the options a client is handed, adding a
 * passkey to an account and signing in with it. No request after the first carries a session, which
 * is how a native client calls, since it keeps no cookie jar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class WebAuthnControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var appConfigService: AppConfigService

    @Autowired
    private lateinit var credentials: PublicKeyCredentialRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun setUp() {
        appConfigService.appConfigProvider.save(
            AppConfig(googleAuthClientId = "test-client-id", rpId = RP_ID, name = "Example")
        )
    }

    @AfterEach
    fun tearDown() {
        appConfigService.appConfigProvider.clearCache()
    }

    private fun signedInUser(): Pair<ObjectId, Cookie> {
        val email = "passkey-${UUID.randomUUID()}@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        return ObjectId(userId) to TestAuthHelpers.loginCookie(mockMvc, json, email)
    }

    private fun loginOptions(): String {
        val body = mockMvc.get("/${ApiEndpoints.LOGIN_OPTIONS}?platform=${Platform.ANDROID.name}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return json.parseToJsonElement(body).jsonObject.getValue("publicKeyAuthOptionsJson").jsonPrimitive.content
    }

    private fun registrationOptions(cookie: Cookie): String =
        mockMvc.post("/webauthn/register/options") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString

    private fun register(cookie: Cookie, credential: JsonObject, label: String = "Test phone"): ResultActionsDsl =
        mockMvc.post("/webauthn/register") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = buildJsonObject {
                put("publicKey", buildJsonObject {
                    put("credential", credential)
                    put("label", label)
                })
            }.toString()
        }

    private fun signIn(assertion: String): ResultActionsDsl =
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(Credential.PublicKey(assertion))
        }

    private fun JsonObject.string(name: String): String {
        val value = assertNotNull(this[name], "$name missing from $this").jsonPrimitive
        assertTrue(value.isString, "$name is not a string in $this")
        return value.content
    }

    private fun JsonObject.number(name: String): Long {
        val value = assertNotNull(this[name], "$name missing from $this") as JsonPrimitive
        assertTrue(!value.isString && value.longOrNull != null, "$name is not a number in $this")
        return value.longOrNull!!
    }

    @Test
    fun `login options carry sign-in options and open no session`() {
        val result = mockMvc.get("/${ApiEndpoints.LOGIN_OPTIONS}?platform=${Platform.WEB.name}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.googleClientId") { value("test-client-id") }
        }.andReturn()

        val body = json.parseToJsonElement(result.response.contentAsString).jsonObject
        val options = json.parseToJsonElement(body.string("publicKeyAuthOptionsJson")).jsonObject
        assertTrue(Regex("[A-Za-z0-9_-]+").matches(options.string("challenge")), options.toString())
        assertEquals(RP_ID, options.string("rpId"))
        assertEquals("preferred", options.string("userVerification"))
        assertTrue(options.number("timeout") > 0)
        assertNull(result.request.getSession(false))
        assertNull(result.response.getHeader("Set-Cookie"))
    }

    @Test
    fun `registration options are only offered to a signed-in account`() {
        mockMvc.post("/webauthn/register/options") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `registration options are in the WebAuthn JSON shape`() {
        val (userId, cookie) = signedInUser()

        val options = json.parseToJsonElement(registrationOptions(cookie)).jsonObject

        assertEquals(RP_ID, options.getValue("rp").jsonObject.string("id"))
        assertEquals(userId.toHexString(), options.getValue("user").jsonObject.string("name"))
        assertTrue(Regex("[A-Za-z0-9_-]{43}").matches(options.getValue("user").jsonObject.string("id")))
        assertEquals("Test User", options.getValue("user").jsonObject.string("displayName"))
        assertTrue(Regex("[A-Za-z0-9_-]+").matches(options.string("challenge")))
        assertTrue(options.number("timeout") > 0)
        assertTrue(-7L in options.getValue("pubKeyCredParams").jsonArray.map { it.jsonObject.number("alg") })
        assertEquals("preferred", options.getValue("authenticatorSelection").jsonObject.string("userVerification"))
    }

    @Test
    fun `a passkey added to an account signs that account in, once per challenge`() {
        val (userId, cookie) = signedInUser()
        val authenticator = TestAuthenticator(ORIGIN)

        val credential = authenticator.create(registrationOptions(cookie))
        register(cookie, credential).andExpect {
            status { isOk() }
            jsonPath("$.message") { value("Passkey added") }
        }
        register(cookie, credential).andExpect { status { isBadRequest() } }

        val stored = assertNotNull(credentials.findById(Bytes(authenticator.credentialId)).orElse(null))
        assertEquals(userId, stored.userId)
        assertEquals("Test phone", stored.label)

        val assertion = authenticator.get(loginOptions())
        signIn(assertion).andExpect {
            status { isOk() }
            jsonPath("$.id") { value(userId.toHexString()) }
        }
        signIn(assertion).andExpect { status { isForbidden() } }

        val used = credentials.findById(Bytes(authenticator.credentialId)).orElseThrow()
        assertEquals(1, used.signatureCount)
        assertEquals(stored.createdAt.truncatedTo(ChronoUnit.MILLIS), used.createdAt.truncatedTo(ChronoUnit.MILLIS))
    }

    @Test
    fun `registration options issued to one account cannot add a passkey to another`() {
        val (_, owner) = signedInUser()
        val (_, other) = signedInUser()
        val authenticator = TestAuthenticator(ORIGIN)

        val credential = authenticator.create(registrationOptions(owner))

        register(other, credential).andExpect { status { isBadRequest() } }
        register(owner, credential).andExpect { status { isOk() } }
    }

    @Test
    fun `a passkey answer from an origin the relying party does not allow is refused`() {
        val (_, cookie) = signedInUser()
        val authenticator = TestAuthenticator(ORIGIN)
        register(cookie, authenticator.create(registrationOptions(cookie))).andExpect { status { isOk() } }

        signIn(authenticator.get(loginOptions(), origin = "https://elsewhere.example")).andExpect {
            status { isForbidden() }
        }
        val foreign = TestAuthenticator("https://elsewhere.example").create(registrationOptions(cookie))
        register(cookie, foreign).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `an unreadable passkey answer is a bad request`() {
        signIn("{}").andExpect { status { isBadRequest() } }
        signIn("not json").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `sign-in options expire with their timeout`() {
        val options = json.parseToJsonElement(loginOptions()).jsonObject
        val stored = mongoTemplate.findAll(WebAuthnChallenge::class.java)
            .single { it.optionsJson.contains(options.string("challenge")) }

        val lifetime = Duration.between(stored.createdAt, stored.expiresAt)
        assertEquals(options.number("timeout"), lifetime.toMillis())
        assertEquals(WebAuthnChallenge.Ceremony.AUTHENTICATION, stored.ceremony)
        assertNull(stored.userId)
    }

    private companion object {
        const val RP_ID = "example.com"
        const val ORIGIN = "https://example.com"
    }
}
