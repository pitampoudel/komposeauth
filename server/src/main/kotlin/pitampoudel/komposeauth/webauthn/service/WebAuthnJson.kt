package pitampoudel.komposeauth.webauthn.service

import org.springframework.security.web.webauthn.api.AttestationConveyancePreference
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInput
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredential
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement
import org.springframework.security.web.webauthn.api.UserVerificationRequirement
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/**
 * Spring's WebAuthn types in the JSON shape the WebAuthn API speaks: base64url strings for bytes,
 * plain strings for the enumerations and milliseconds for a timeout. Spring Security's own Jackson
 * module writes that shape, and reads what a client sends back, but it cannot read the options
 * themselves (they have builders and no creators), so those are rebuilt here from the JSON that was
 * handed out — which is what lets the store keep exactly what the client was given.
 */
@Component
class WebAuthnJson {
    private val mapper: JsonMapper = JsonMapper.builder().addModule(WebauthnJacksonModule()).build()

    fun write(options: PublicKeyCredentialRequestOptions): String = mapper.writeValueAsString(options)

    fun write(options: PublicKeyCredentialCreationOptions): String = mapper.writeValueAsString(options)

    fun readAssertion(json: String): PublicKeyCredential<AuthenticatorAssertionResponse> =
        mapper.readValue(json, object : TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {})

    /** A registration body: `{"publicKey": {"credential": {...}, "label": "..."}}`. */
    fun readRegistration(json: String): RelyingPartyPublicKey =
        mapper.treeToValue(mapper.readTree(json).required("publicKey"), RelyingPartyPublicKey::class.java)

    /** The challenge a client signed over, as its `clientDataJSON` names it; null when it names none. */
    fun challengeOf(clientDataJSON: Bytes): Bytes? = runCatching {
        mapper.readTree(clientDataJSON.bytes).get("challenge")
            ?.takeIf { it.isString }
            ?.let { Bytes.fromBase64(it.stringValue()) }
    }.getOrNull()

    fun readRequestOptions(json: String): PublicKeyCredentialRequestOptions {
        val node = mapper.readTree(json)
        return PublicKeyCredentialRequestOptions.builder()
            .challenge(Bytes.fromBase64(node.required("challenge").stringValue()))
            .apply {
                node.present("timeout")?.let { timeout(Duration.ofMillis(it.longValue())) }
                node.present("rpId")?.let { rpId(it.stringValue()) }
                node.present("allowCredentials")?.let { allowCredentials(it.map(::descriptor)) }
                node.present("userVerification")?.let { userVerification(userVerification(it.stringValue())) }
                node.present("extensions")?.let { extensions(extensions(it)) }
            }
            .build()
    }

    fun readCreationOptions(json: String): PublicKeyCredentialCreationOptions {
        val node = mapper.readTree(json)
        val rp = node.required("rp")
        val user = node.required("user")
        return PublicKeyCredentialCreationOptions.builder()
            .rp(
                PublicKeyCredentialRpEntity.builder()
                    .name(rp.required("name").stringValue())
                    .id(rp.required("id").stringValue())
                    .build()
            )
            .user(
                ImmutablePublicKeyCredentialUserEntity.builder()
                    .id(Bytes.fromBase64(user.required("id").stringValue()))
                    .name(user.required("name").stringValue())
                    .apply { user.present("displayName")?.let { displayName(it.stringValue()) } }
                    .build()
            )
            .challenge(Bytes.fromBase64(node.required("challenge").stringValue()))
            .pubKeyCredParams(node.required("pubKeyCredParams").map { param ->
                val alg = param.required("alg").longValue()
                PARAMETERS.single { it.alg.value == alg }
            })
            .apply {
                node.present("timeout")?.let { timeout(Duration.ofMillis(it.longValue())) }
                node.present("excludeCredentials")?.let { excludeCredentials(it.map(::descriptor)) }
                node.present("authenticatorSelection")?.let { authenticatorSelection(selection(it)) }
                node.present("attestation")?.let { attestation(AttestationConveyancePreference.valueOf(it.stringValue())) }
                node.present("extensions")?.let { extensions(extensions(it)) }
            }
            .build()
    }

    private fun JsonNode.present(name: String): JsonNode? = get(name)?.takeUnless { it.isNull || it.isMissingNode }

    private fun descriptor(node: JsonNode): PublicKeyCredentialDescriptor =
        PublicKeyCredentialDescriptor.builder()
            .type(PublicKeyCredentialType.valueOf(node.required("type").stringValue()))
            .id(Bytes.fromBase64(node.required("id").stringValue()))
            .apply {
                node.present("transports")?.let { transports ->
                    transports(transports.map { AuthenticatorTransport.valueOf(it.stringValue()) }.toSet())
                }
            }
            .build()

    private fun selection(node: JsonNode): AuthenticatorSelectionCriteria =
        AuthenticatorSelectionCriteria.builder()
            .apply {
                node.present("authenticatorAttachment")?.let {
                    authenticatorAttachment(AuthenticatorAttachment.valueOf(it.stringValue()))
                }
                node.present("residentKey")?.let { residentKey(ResidentKeyRequirement.valueOf(it.stringValue())) }
                node.present("userVerification")?.let { userVerification(userVerification(it.stringValue())) }
            }
            .build()

    /**
     * One of Spring's own constants, never an equal copy: the relying party compares the requirement
     * by identity when it decides whether user verification is required.
     */
    private fun userVerification(value: String): UserVerificationRequirement =
        USER_VERIFICATION.single { it.value == value }

    private fun extensions(node: JsonNode): AuthenticationExtensionsClientInputs =
        ImmutableAuthenticationExtensionsClientInputs(node.properties().map { (id, value) ->
            if (id == ImmutableAuthenticationExtensionsClientInput.credProps.extensionId) {
                ImmutableAuthenticationExtensionsClientInput.credProps
            } else {
                ImmutableAuthenticationExtensionsClientInput(id, mapper.treeToValue(value, Any::class.java))
            } as AuthenticationExtensionsClientInput<*>
        })

    private companion object {
        val USER_VERIFICATION = listOf(
            UserVerificationRequirement.DISCOURAGED,
            UserVerificationRequirement.PREFERRED,
            UserVerificationRequirement.REQUIRED
        )
        val PARAMETERS = listOf(
            PublicKeyCredentialParameters.EdDSA,
            PublicKeyCredentialParameters.ES256,
            PublicKeyCredentialParameters.ES384,
            PublicKeyCredentialParameters.ES512,
            PublicKeyCredentialParameters.RS256,
            PublicKeyCredentialParameters.RS384,
            PublicKeyCredentialParameters.RS512,
            PublicKeyCredentialParameters.RS1
        )
    }
}
