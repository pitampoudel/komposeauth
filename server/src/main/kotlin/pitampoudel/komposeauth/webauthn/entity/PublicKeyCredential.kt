package pitampoudel.komposeauth.webauthn.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.PublicKeyCose
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import java.time.Instant

@Document(collection = "public_key_credentials")
@TypeAlias("public_key_credential")
data class PublicKeyCredential(
    @Id
    val id: Bytes,
    @Indexed
    val publicKeyUserId: Bytes,
    @get:JvmName("labelValue")
    val label: String,
    @get:JvmName("signatureCountValue")
    val signatureCount: Long,
    @get:JvmName("credentialTypeValue")
    val credentialType: PublicKeyCredentialType,
    @get:JvmName("publicKeyValue")
    val publicKey: PublicKeyCose,
    @get:JvmName("attestationClientDataJSONValue")
    val attestationClientDataJSON: Bytes,
    @get:JvmName("attestationObjectValue")
    val attestationObject: Bytes,
    @get:JvmName("transportsValue")
    val transports: Set<AuthenticatorTransport>,
    val backupEligible: Boolean,
    val backupState: Boolean,
    val uvInitialized: Boolean,
    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    var updatedAt: Instant = Instant.now(),
    @get:JvmName("lastUsedValue")
    val lastUsed: Instant?
) : CredentialRecord {
    override fun getCredentialType(): PublicKeyCredentialType {
        return credentialType
    }

    override fun getCredentialId(): Bytes {
        return id
    }

    override fun getPublicKey(): PublicKeyCose {
        return publicKey
    }

    override fun getSignatureCount(): Long {
        return signatureCount
    }

    override fun isUvInitialized(): Boolean {
        return uvInitialized
    }

    override fun getTransports(): Set<AuthenticatorTransport> {
        return transports
    }

    override fun isBackupEligible(): Boolean {
        return backupEligible
    }

    override fun isBackupState(): Boolean {
        return backupState
    }

    override fun getUserEntityUserId(): Bytes {
        return publicKeyUserId
    }

    override fun getAttestationObject(): Bytes {
        return attestationObject
    }

    override fun getAttestationClientDataJSON(): Bytes {
        return attestationClientDataJSON
    }

    override fun getLabel(): String? {
        return label
    }

    override fun getLastUsed(): Instant? {
        return lastUsed
    }

    override fun getCreated(): Instant {
        return createdAt
    }
}