package pitampoudel.komposeauth.webauthn.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
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
@CompoundIndexes(
    CompoundIndex(
        name = "user_created_idx",
        def = "{'publicKeyUserId': 1, 'createdAt': -1}"
    ),
    CompoundIndex(
        name = "user_lastused_idx",
        def = "{'publicKeyUserId': 1, 'lastUsedAt': -1}"
    ),
    CompoundIndex(
        name = "userid_lastused_idx",
        def = "{'userId': 1, 'lastUsedAt': -1}"
    )
)
data class PublicKeyCredential(
    /** WebAuthn credentialId. */
    @Id
    val id: Bytes,

    /** WebAuthn user handle. */
    @Indexed
    val publicKeyUserId: Bytes,

    /** Reference to `users._id` for fast cleanup/queries (optional but recommended). */
    @Indexed
    val userId: ObjectId,

    @get:JvmName("labelValue")
    val label: String,

    @get:JvmName("signatureCountValue")
    val signatureCount: Long,

    @get:JvmName("credentialTypeValue")
    val credentialType: PublicKeyCredentialType?,

    @get:JvmName("publicKeyValue")
    val publicKey: PublicKeyCose,

    /** Optional: store only if you need attestation troubleshooting/compliance. */
    @get:JvmName("attestationClientDataJSONValue")
    val attestationClientDataJSON: Bytes?,

    /** Optional: store only if you need attestation troubleshooting/compliance. */
    @get:JvmName("attestationObjectValue")
    val attestationObject: Bytes?,

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
    val lastUsedAt: Instant
) : CredentialRecord {
    override fun getCredentialType(): PublicKeyCredentialType? = credentialType

    override fun getCredentialId(): Bytes = id

    override fun getPublicKey(): PublicKeyCose = publicKey

    override fun getSignatureCount(): Long = signatureCount

    override fun isUvInitialized(): Boolean = uvInitialized

    override fun getTransports(): Set<AuthenticatorTransport> = transports

    override fun isBackupEligible(): Boolean = backupEligible

    override fun isBackupState(): Boolean = backupState

    override fun getUserEntityUserId(): Bytes = publicKeyUserId

    override fun getAttestationObject(): Bytes? = attestationObject

    override fun getAttestationClientDataJSON(): Bytes? = attestationClientDataJSON

    override fun getLabel(): String = label

    override fun getLastUsed(): Instant = lastUsedAt

    override fun getCreated(): Instant = createdAt
}