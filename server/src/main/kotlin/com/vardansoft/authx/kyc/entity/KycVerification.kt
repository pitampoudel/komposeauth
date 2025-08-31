package com.vardansoft.authx.kyc.entity

import com.vardansoft.authx.data.KycResponse
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "kyc_verifications")
@TypeAlias("kyc_verification")
data class KycVerification(
    @Id val id: ObjectId = ObjectId.get(),
    @Indexed(unique = true) val userId: ObjectId,
    val fullName: String,
    val documentType: String,
    val documentNumber: String,
    val country: String,
    val status: KycResponse.Status = KycResponse.Status.PENDING,
    val remarks: String? = null,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
    @CreatedDate val createdAt: Instant = Instant.now(),
    @LastModifiedDate val updatedAt: Instant = Instant.now()
) {

}
