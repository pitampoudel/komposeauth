package com.vardansoft.komposeauth.webauthn

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity

@Document(collection = "public_key_users")
@TypeAlias("public_key_user")
data class PublicKeyUser(
    @Id
    val id: String,
    @get:JvmName("nameValue")
    val name: String,
    @get:JvmName("displayNameValue")
    val displayName: String
) : PublicKeyCredentialUserEntity {
    override fun getId(): Bytes = Bytes.fromBase64(id)
    override fun getName(): String = name
    override fun getDisplayName(): String = displayName
}