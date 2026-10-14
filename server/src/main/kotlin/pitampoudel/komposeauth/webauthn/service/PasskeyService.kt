package pitampoudel.komposeauth.webauthn.service

import com.webauthn4j.util.exception.WebAuthnException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyUserRepository
import tools.jackson.core.JacksonException
import kotlin.jvm.optionals.getOrNull

/**
 * Signing in with a passkey and adding one. The options of each ceremony are kept server-side and
 * found again by the challenge the client's answer carries, so finishing one needs nothing from
 * the client but the answer itself.
 */
@Service
class PasskeyService(
    private val rpOperations: WebAuthnRelyingPartyOperations,
    private val challenges: WebAuthnChallengeStore,
    private val webAuthnJson: WebAuthnJson,
    private val publicKeyUserRepository: PublicKeyUserRepository,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Options for signing in with any passkey of this relying party, in the WebAuthn JSON shape. */
    fun signInOptions(): String {
        val options = rpOperations.createCredentialRequestOptions(ImmutablePublicKeyCredentialRequestOptionsRequest(null))
        challenges.save(options)
        return webAuthnJson.write(options)
    }

    /**
     * The user whose passkey signed [authenticationResponseJson], or null when the signature, origin
     * or credential does not check out.
     */
    fun signIn(authenticationResponseJson: String): User? {
        val credential = try {
            webAuthnJson.readAssertion(authenticationResponseJson)
        } catch (ex: JacksonException) {
            log.debug("Unreadable passkey assertion", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, UNREADABLE)
        }
        val challenge = credential.response.clientDataJSON.let(webAuthnJson::challengeOf)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, UNREADABLE)
        val options = challenges.takeRequestOptions(challenge)
            ?: throw AccessDeniedException("This passkey sign-in has expired. Try again.")
        val userEntity = try {
            rpOperations.authenticate(RelyingPartyAuthenticationRequest(options, credential))
        } catch (ex: WebAuthnException) {
            log.info("Passkey sign-in refused: {}", ex.toString())
            return null
        } catch (ex: IllegalArgumentException) {
            // Spring's answer for a credential it has no record of, such as one removed since.
            log.info("Passkey sign-in refused: {}", ex.message)
            return null
        }
        val userId = publicKeyUserRepository.findByUserHandle(userEntity.id)?.userId ?: return null
        return userRepository.findById(userId).getOrNull()
    }

    /** Options for adding a passkey to [user]'s account, in the WebAuthn JSON shape. */
    fun registrationOptions(user: User): String {
        // Named by the account's id, which is what the passkey's user handle is looked up by.
        val authentication = UsernamePasswordAuthenticationToken.authenticated(user.id.toHexString(), null, emptyList())
        val options = rpOperations.createPublicKeyCredentialCreationOptions(
            ImmutablePublicKeyCredentialCreationOptionsRequest(authentication)
        )
        challenges.save(user.id, options)
        return webAuthnJson.write(options)
    }

    /** Verifies [registrationJson] against the options [user] was given and keeps the passkey. */
    fun register(user: User, registrationJson: String) {
        val publicKey = try {
            webAuthnJson.readRegistration(registrationJson)
        } catch (ex: JacksonException) {
            log.debug("Unreadable passkey registration", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, UNREADABLE)
        }
        val challenge = publicKey.credential.response?.clientDataJSON?.let(webAuthnJson::challengeOf)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, UNREADABLE)
        val options = challenges.takeCreationOptions(challenge, user.id)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "This passkey request has expired. Try again.")
        val labelled = RelyingPartyPublicKey(publicKey.credential, label(publicKey.label))
        try {
            rpOperations.registerCredential(ImmutableRelyingPartyRegistrationRequest(options, labelled))
        } catch (ex: WebAuthnException) {
            // A wrong origin is the usual cause, and it is a configuration mistake worth seeing.
            log.warn("Passkey registration refused: {}", ex.toString())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "This passkey could not be verified.")
        } catch (ex: IllegalArgumentException) {
            log.info("Passkey registration refused: {}", ex.message)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "This passkey could not be added.")
        }
    }

    /** What the account's list of passkeys will call it: the name the client gave, within reason. */
    private fun label(requested: String?): String =
        requested?.trim()?.take(MAX_LABEL_LENGTH)?.takeIf { it.isNotEmpty() } ?: "Passkey"

    private companion object {
        const val UNREADABLE = "The passkey response could not be read."
        const val MAX_LABEL_LENGTH = 64
    }
}
