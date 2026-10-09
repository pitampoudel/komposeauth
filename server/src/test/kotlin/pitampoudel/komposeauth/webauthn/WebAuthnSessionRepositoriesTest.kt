package pitampoudel.komposeauth.webauthn

import com.fasterxml.jackson.core.type.TypeReference
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredential
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.SerializationConfig
import pitampoudel.komposeauth.webauthn.config.WebAuthnConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The options a passkey ceremony starts with wait in the session for the browser's answer, and the
 * session store serializes its attributes with Java serialization. So each repository is checked
 * across that round trip, and the options are compared in the JSON they are sent to the browser in.
 */
class WebAuthnSessionRepositoriesTest {

    private val mapper = SerializationConfig().objectMapper()
    private val config = WebAuthnConfig(mockk<AppConfigService>())

    private val operations = Webauthn4JRelyingPartyOperations(
        mockk<PublicKeyCredentialUserEntityRepository> {
            every { findByUsername("ada") } returns ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.random()).name("ada").displayName("Ada Lovelace").build()
        },
        mockk<UserCredentialRepository> { every { findByUserId(any()) } returns emptyList() },
        PublicKeyCredentialRpEntity.builder().id("example.com").name("Example").build(),
        setOf("https://example.com")
    )

    /** What the session store does to every attribute between two requests. */
    private fun HttpSession.storedAndReloaded(): MockHttpSession {
        val reloaded = MockHttpSession()
        attributeNames.toList().forEach { name ->
            val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(getAttribute(name)) } }
            val value = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
            reloaded.setAttribute(name, value)
        }
        return reloaded
    }

    @Test
    fun `sign-in options survive the session store`() {
        val repository = config.requestOptionsRepository()
        val options = operations.createCredentialRequestOptions(ImmutablePublicKeyCredentialRequestOptionsRequest(null))
        val first = MockHttpServletRequest()
        repository.save(first, MockHttpServletResponse(), options)

        val next = MockHttpServletRequest().apply { setSession(first.session!!.storedAndReloaded()) }
        val loaded = assertNotNull(repository.load(next))

        assertEquals(mapper.writeValueAsString(options), mapper.writeValueAsString(loaded))
    }

    @Test
    fun `registration options survive the session store`() {
        val repository = config.publicKeyCredentialCreationOptionsRepository()
        val options = operations.createPublicKeyCredentialCreationOptions(
            ImmutablePublicKeyCredentialCreationOptionsRequest(TestingAuthenticationToken("ada", "", "ROLE_USER"))
        )
        val first = MockHttpServletRequest()
        repository.save(first, MockHttpServletResponse(), options)

        val next = MockHttpServletRequest().apply { setSession(first.session!!.storedAndReloaded()) }
        val loaded = assertNotNull(repository.load(next))

        assertEquals(mapper.writeValueAsString(options), mapper.writeValueAsString(loaded))

        repository.save(next, MockHttpServletResponse(), null)
        assertNull(repository.load(next))
    }

    @Test
    fun `options are written in the WebAuthn JSON shape`() {
        val options = operations.createCredentialRequestOptions(ImmutablePublicKeyCredentialRequestOptionsRequest(null))

        val json = mapper.readTree(mapper.writeValueAsString(options))

        assertTrue(json["challenge"].isTextual, json.toString())
        assertEquals(options.challenge.toBase64UrlString(), json["challenge"].asText())
        assertTrue(json["userVerification"].isTextual, json.toString())
    }

    @Test
    fun `a passkey assertion is read off the wire`() {
        val raw = Bytes.random().toBase64UrlString()
        val assertion = """
            {"id":"$raw","rawId":"$raw","type":"public-key","clientExtensionResults":{},
             "authenticatorAttachment":"platform",
             "response":{"authenticatorData":"$raw","clientDataJSON":"$raw","signature":"$raw","userHandle":"$raw"}}
        """.trimIndent()

        val credential = mapper.readValue(
            assertion,
            object : TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {}
        )

        assertEquals(raw, credential.rawId.toBase64UrlString())
        assertEquals(raw, credential.response.signature.toBase64UrlString())
    }
}
