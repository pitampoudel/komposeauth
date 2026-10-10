package pitampoudel.komposeauth.webauthn.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.web.webauthn.api.*
import org.springframework.security.web.webauthn.authentication.HttpSessionPublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest
import org.springframework.security.web.webauthn.management.PublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.webauthn.entity.PublicKeyCredential
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import pitampoudel.komposeauth.webauthn.repository.PublicKeyCredentialRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyUserRepository
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.optionals.getOrNull

/**
 * Keeps the registration options in the session as their serializable parts. Spring's own session
 * repository stores the options object itself, which is not [Serializable], and the session store
 * serializes every attribute.
 */
private class SessionCreationOptionsRepository : PublicKeyCredentialCreationOptionsRepository {
    override fun save(
        request: HttpServletRequest,
        response: HttpServletResponse,
        options: PublicKeyCredentialCreationOptions?
    ) {
        if (options == null) {
            request.getSession(false)?.removeAttribute(ATTR)
            return
        }
        request.getSession(true).setAttribute(ATTR, StoredCreationOptions(options))
    }

    override fun load(request: HttpServletRequest): PublicKeyCredentialCreationOptions? =
        (request.getSession(false)?.getAttribute(ATTR) as? StoredCreationOptions)?.toOptions()

    private class StoredCreationOptions(options: PublicKeyCredentialCreationOptions) : Serializable {
        private val rpId = options.rp.id
        private val rpName = options.rp.name
        private val user: PublicKeyCredentialUserEntity = ImmutablePublicKeyCredentialUserEntity.builder()
            .id(options.user.id).name(options.user.name)
            .apply { options.user.displayName?.let { displayName(it) } }
            .build()
        private val challenge = options.challenge
        private val algorithms = options.pubKeyCredParams.map { it.alg.value }
        private val timeout = options.timeout
        private val excludeCredentials = options.excludeCredentials?.let(::ArrayList)
        private val authenticatorAttachment = options.authenticatorSelection?.authenticatorAttachment
        private val residentKey = options.authenticatorSelection?.residentKey?.value
        private val userVerification = options.authenticatorSelection?.userVerification
        private val hasAuthenticatorSelection = options.authenticatorSelection != null
        private val attestation = options.attestation?.value
        private val extensions = options.extensions

        fun toOptions(): PublicKeyCredentialCreationOptions = PublicKeyCredentialCreationOptions.builder()
            .rp(PublicKeyCredentialRpEntity.builder().id(rpId).name(rpName).build())
            .user(user)
            .challenge(challenge)
            .pubKeyCredParams(algorithms.map { alg -> PARAMETERS.single { it.alg.value == alg } })
            .apply {
                timeout?.let { timeout(it) }
                excludeCredentials?.let { excludeCredentials(it) }
                if (hasAuthenticatorSelection) authenticatorSelection(
                    AuthenticatorSelectionCriteria.builder().apply {
                        authenticatorAttachment?.let { authenticatorAttachment(it) }
                        residentKey?.let { residentKey(ResidentKeyRequirement.valueOf(it)) }
                        userVerification?.let { userVerification(it) }
                    }.build()
                )
                attestation?.let { attestation(AttestationConveyancePreference.valueOf(it)) }
                extensions?.let { extensions(it) }
            }
            .build()
    }

    private companion object {
        const val ATTR = "WEBAUTHN_CREATION_OPTIONS"
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

private fun stableWebAuthnUserHandle(userId: ObjectId): Bytes {
    // 32 bytes, stable. Purpose-separated so it doesn't collide with other hashes.
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes =
        digest.digest("komposeauth:webauthn:userHandle:${userId.toHexString()}".toByteArray(StandardCharsets.UTF_8))
    return Bytes(bytes)
}

@Repository
class UserCredentialRepositoryImpl(
    private val repository: PublicKeyCredentialRepository,
    private val publicKeyUserRepository: PublicKeyUserRepository
) : UserCredentialRepository {
    override fun delete(credentialId: Bytes) {
        repository.deleteById(credentialId)
    }

    override fun save(credentialRecord: CredentialRecord) {
        // Best-effort denormalization: map userHandle -> userId.
        val publicKeyUser = publicKeyUserRepository.findByUserHandle(credentialRecord.userEntityUserId) ?: return
        val userId = publicKeyUser.userId

        repository.save(
            PublicKeyCredential(
                id = credentialRecord.credentialId,
                publicKeyUserId = credentialRecord.userEntityUserId,
                userId = userId,
                label = credentialRecord.label,
                attestationClientDataJSON = credentialRecord.attestationClientDataJSON,
                attestationObject = credentialRecord.attestationObject,
                signatureCount = credentialRecord.signatureCount,
                transports = credentialRecord.transports,
                publicKey = credentialRecord.publicKey,
                backupEligible = credentialRecord.isBackupEligible,
                backupState = credentialRecord.isBackupState,
                uvInitialized = credentialRecord.isUvInitialized,
                credentialType = credentialRecord.credentialType,
                lastUsedAt = credentialRecord.lastUsed
            )
        )
    }

    override fun findByCredentialId(credentialId: Bytes): CredentialRecord? {
        return repository.findById(credentialId).getOrNull()
    }

    override fun findByUserId(userId: Bytes): List<CredentialRecord> {
        return repository.findAllByPublicKeyUserId(userId)
    }

}

@Repository
class PublicKeyCredentialUserEntityRepositoryImpl(
    val userRepository: UserRepository,
    private val repository: PublicKeyUserRepository
) : PublicKeyCredentialUserEntityRepository {
    override fun findById(id: Bytes): PublicKeyCredentialUserEntity? {
        return repository.findByUserHandle(id)
    }

    override fun findByUsername(username: String): PublicKeyCredentialUserEntity? {
        var record = repository.findByName(username)
        if (record == null) {
            val user = userRepository.findByUserName(username) ?: return null
            record = PublicKeyUser(
                userId = user.id,
                userHandle = stableWebAuthnUserHandle(user.id),
                name = username,
                displayName = user.fullName,
                id = ObjectId()
            )
            repository.save(record)
        }
        return record
    }

    override fun save(userEntity: PublicKeyCredentialUserEntity) {
        // This repository is only used by the WebAuthn flow; we still enforce that the username maps to a real user.
        val user = userRepository.findByUserName(userEntity.name) ?: return
        repository.save(
            PublicKeyUser(
                userId = user.id,
                userHandle = userEntity.id,
                name = userEntity.name,
                displayName = userEntity.displayName,
                id = ObjectId()
            )
        )
    }

    override fun delete(id: Bytes) {
        val existing = repository.findByUserHandle(id) ?: return
        repository.deleteById(existing.id)
    }

}

@Configuration
class WebAuthnConfig(
    private val appConfigService: AppConfigService
) {
    @Bean
    fun requestOptionsRepository(): PublicKeyCredentialRequestOptionsRepository =
        HttpSessionPublicKeyCredentialRequestOptionsRepository()

    @Bean
    fun publicKeyCredentialCreationOptionsRepository(): PublicKeyCredentialCreationOptionsRepository =
        SessionCreationOptionsRepository()

    @Bean
    fun relyingPartyOperations(
        userCredentialRepository: UserCredentialRepository,
        userEntityRepository: PublicKeyCredentialUserEntityRepository
    ): WebAuthnRelyingPartyOperations =
        ConfiguredRelyingPartyOperations(appConfigService, userEntityRepository, userCredentialRepository)

}

/**
 * The relying party's id, name and origins are set on the config page of a running server, so the
 * operations are rebuilt when they change instead of being fixed at boot, where a deployment
 * configured after startup offered passkeys for `localhost` until it was restarted.
 */
private class ConfiguredRelyingPartyOperations(
    private val appConfigService: AppConfigService,
    private val userEntities: PublicKeyCredentialUserEntityRepository,
    private val credentials: UserCredentialRepository
) : WebAuthnRelyingPartyOperations {
    private data class Settings(val rpId: String, val name: String, val origins: Set<String>)

    private val current = AtomicReference<Pair<Settings, WebAuthnRelyingPartyOperations>?>(null)

    private fun operations(): WebAuthnRelyingPartyOperations {
        val settings = Settings(
            rpId = appConfigService.rpId() ?: "localhost",
            name = appConfigService.getConfig().name ?: "komposeauth",
            origins = appConfigService.webauthnAllowedOrigins()
        )
        current.get()?.takeIf { it.first == settings }?.let { return it.second }
        val built = Webauthn4JRelyingPartyOperations(
            userEntities,
            credentials,
            PublicKeyCredentialRpEntity.builder().id(settings.rpId).name(settings.name).build(),
            settings.origins
        )
        current.set(settings to built)
        return built
    }

    override fun createPublicKeyCredentialCreationOptions(request: PublicKeyCredentialCreationOptionsRequest) =
        operations().createPublicKeyCredentialCreationOptions(request)

    override fun registerCredential(request: RelyingPartyRegistrationRequest) =
        operations().registerCredential(request)

    override fun createCredentialRequestOptions(request: PublicKeyCredentialRequestOptionsRequest) =
        operations().createCredentialRequestOptions(request)

    override fun authenticate(request: RelyingPartyAuthenticationRequest) =
        operations().authenticate(request)
}
