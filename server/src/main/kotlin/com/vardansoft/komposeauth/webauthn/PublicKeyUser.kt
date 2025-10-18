package com.vardansoft.komposeauth.webauthn

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Document(collection = "public_key_users")
@TypeAlias("public_key_user")
data class PublicKeyUser(
    @Id
    val id: String,
    @get:JvmName("emailValue")
    val email: String,
    @get:JvmName("displayNameValue")
    val displayName: String
) : PublicKeyCredentialUserEntity {
    override fun getId(): Bytes = Bytes.fromBase64(id)
    override fun getName(): String = email
    override fun getDisplayName(): String = displayName
}

@Repository
interface MongoPublicKeyCredentialUserEntityRepository :
    MongoRepository<PublicKeyUser, String> {
    fun findByEmail(email: String): PublicKeyUser?
}

@Repository
class PublicKeyCredentialUserEntityRepositoryImpl(
    private val repository: MongoPublicKeyCredentialUserEntityRepository
) : PublicKeyCredentialUserEntityRepository {
    override fun findById(id: Bytes): PublicKeyCredentialUserEntity? {
        return repository.findById(id.toBase64UrlString()).getOrNull()
    }

    override fun findByUsername(username: String): PublicKeyCredentialUserEntity? {
        return repository.findByEmail(username)
    }

    override fun save(userEntity: PublicKeyCredentialUserEntity) {
        repository.save(
            PublicKeyUser(
                id = userEntity.id.toBase64UrlString(),
                email = userEntity.name,
                displayName = userEntity.displayName
            )
        )
    }

    override fun delete(id: Bytes) {
        repository.deleteById(id.toBase64UrlString())
    }

}