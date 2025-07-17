package com.vardansoft.auth.credentials.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "credentials")
@TypeAlias("credential")
@CompoundIndex(def = "{'provider' : 1, 'userId': 1}", name = "unique_provider_userid", unique = true)
data class Credential(
    @Id val id: ObjectId,
    val userId: ObjectId,
    val provider: Provider,
    val accessToken: String,
    val refreshToken: String?,
    val createdAt: Instant = Instant.now()
) {
    enum class Provider {
        FACEBOOK, GOOGLE
    }
}