package pitampoudel.komposeauth.webauthn.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
import java.time.Instant

@Document(collection = "public_key_users")
@TypeAlias("public_key_user")
data class PublicKeyUser(
    @Id
    val id: ObjectId,

    /** Reference to `users._id` (1:1). */
    @Indexed(unique = true)
    val userId: ObjectId,

    /** WebAuthn user handle (stable, unique). */
    @Indexed(unique = true)
    val userHandle: Bytes,

    @get:JvmName("nameValue")
    @Indexed(unique = true)
    val name: String,

    @get:JvmName("displayNameValue")
    val displayName: String?,

    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    val updatedAt: Instant = Instant.now()
) : PublicKeyCredentialUserEntity {
    override fun getId(): Bytes = userHandle
    override fun getName(): String = name
    override fun getDisplayName(): String? = displayName
}