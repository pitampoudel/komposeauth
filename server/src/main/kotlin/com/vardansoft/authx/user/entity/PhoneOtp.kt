package com.vardansoft.authx.user.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "phone_otps")
@TypeAlias("phone_otp")
data class PhoneOtp(
    @Id
    val id: ObjectId = ObjectId(),
    val phoneNumber: String,
    val otp: String,
    @CreatedDate
    @Indexed(expireAfter = "5m") // 5 minutes TTL
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(300) // 5 minutes from now
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
