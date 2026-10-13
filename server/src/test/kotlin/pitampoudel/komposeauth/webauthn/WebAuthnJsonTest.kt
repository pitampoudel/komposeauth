package pitampoudel.komposeauth.webauthn

import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.api.UserVerificationRequirement
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations
import pitampoudel.komposeauth.webauthn.service.WebAuthnJson
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The options are handed to a client in the JSON the WebAuthn API reads, and kept server-side as
 * that same JSON until the client answers, so both directions are checked here: the shape, and that
 * what is read back is what was handed out.
 */
class WebAuthnJsonTest {

    private val webAuthnJson = WebAuthnJson()
    private val handle: Bytes = Bytes.random()
    private val existingPasskey: Bytes = Bytes.random()

    private val operations = Webauthn4JRelyingPartyOperations(
        mockk<PublicKeyCredentialUserEntityRepository> {
            every { findByUsername("ada") } returns ImmutablePublicKeyCredentialUserEntity.builder()
                .id(handle).name("ada").displayName("Ada Lovelace").build()
        },
        mockk<UserCredentialRepository> {
            every { findByUserId(any()) } returns listOf(mockk<CredentialRecord> {
                every { credentialId } returns existingPasskey
                every { transports } returns setOf(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID)
            })
        },
        PublicKeyCredentialRpEntity.builder().id("example.com").name("Example").build(),
        setOf("https://example.com")
    )

    private fun signInOptions() =
        operations.createCredentialRequestOptions(ImmutablePublicKeyCredentialRequestOptionsRequest(null))

    private fun registrationOptions() = operations.createPublicKeyCredentialCreationOptions(
        ImmutablePublicKeyCredentialCreationOptionsRequest(TestingAuthenticationToken("ada", "", "ROLE_USER"))
    )

    private fun parse(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

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
    fun `sign-in options are written in the WebAuthn JSON shape`() {
        val options = signInOptions()

        val json = parse(webAuthnJson.write(options))

        assertEquals(options.challenge.toBase64UrlString(), json.string("challenge"))
        assertTrue(Regex("[A-Za-z0-9_-]+").matches(json.string("challenge")))
        assertEquals("preferred", json.string("userVerification"))
        assertEquals(options.timeout!!.toMillis(), json.number("timeout"))
        assertEquals("example.com", json.string("rpId"))
    }

    @Test
    fun `registration options are written in the WebAuthn JSON shape`() {
        val options = registrationOptions()

        val json = parse(webAuthnJson.write(options))

        assertEquals(options.challenge.toBase64UrlString(), json.string("challenge"))
        assertEquals(options.timeout!!.toMillis(), json.number("timeout"))
        assertEquals("example.com", json.getValue("rp").jsonObject.string("id"))
        assertEquals(handle.toBase64UrlString(), json.getValue("user").jsonObject.string("id"))
        assertEquals("ada", json.getValue("user").jsonObject.string("name"))
        assertEquals("preferred", json.getValue("authenticatorSelection").jsonObject.string("userVerification"))
        assertTrue(-7L in json.getValue("pubKeyCredParams").jsonArray.map { it.jsonObject.number("alg") })
        val excluded = json.getValue("excludeCredentials").jsonArray.single().jsonObject
        assertEquals(existingPasskey.toBase64UrlString(), excluded.string("id"))
    }

    @Test
    fun `sign-in options read back as they were handed out`() {
        val written = webAuthnJson.write(signInOptions())

        val read = webAuthnJson.readRequestOptions(written)

        assertEquals(written, webAuthnJson.write(read))
        assertSame(UserVerificationRequirement.PREFERRED, read.userVerification)
    }

    @Test
    fun `registration options read back as they were handed out`() {
        val written = webAuthnJson.write(registrationOptions())

        val read = webAuthnJson.readCreationOptions(written)

        assertEquals(written, webAuthnJson.write(read))
        assertSame(UserVerificationRequirement.PREFERRED, read.authenticatorSelection.userVerification)
    }

    @Test
    fun `the challenge is read from what the client signed`() {
        val challenge = Bytes.random()
        val clientData = """{"type":"webauthn.get","challenge":"${challenge.toBase64UrlString()}","origin":"https://example.com"}"""

        assertEquals(challenge, webAuthnJson.challengeOf(Bytes(clientData.toByteArray())))
        assertNull(webAuthnJson.challengeOf(Bytes("""{"type":"webauthn.get"}""".toByteArray())))
        assertNull(webAuthnJson.challengeOf(Bytes("not json".toByteArray())))
    }

    @Test
    fun `a passkey assertion is read off the wire`() {
        val raw = Bytes.random().toBase64UrlString()
        val assertion = """
            {"id":"$raw","rawId":"$raw","type":"public-key","clientExtensionResults":{},
             "authenticatorAttachment":"platform",
             "response":{"authenticatorData":"$raw","clientDataJSON":"$raw","signature":"$raw","userHandle":"$raw"}}
        """.trimIndent()

        val credential = webAuthnJson.readAssertion(assertion)

        assertEquals(raw, credential.rawId.toBase64UrlString())
        assertEquals(raw, credential.response.signature.toBase64UrlString())
    }

    @Test
    fun `a passkey registration is read off the wire with its label`() {
        val raw = Bytes.random().toBase64UrlString()
        val registration = """
            {"publicKey":{"label":"Pixel 9","credential":{"id":"$raw","rawId":"$raw","type":"public-key",
             "clientExtensionResults":{"credProps":{"rk":true}},"authenticatorAttachment":"platform",
             "response":{"attestationObject":"$raw","clientDataJSON":"$raw","transports":["internal","hybrid"]}}}}
        """.trimIndent()

        val publicKey = webAuthnJson.readRegistration(registration)

        assertEquals("Pixel 9", publicKey.label)
        assertEquals(raw, publicKey.credential.rawId.toBase64UrlString())
        assertEquals(
            listOf(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID),
            publicKey.credential.response.transports
        )
    }
}
