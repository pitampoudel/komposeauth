package com.vardansoft.authx.kyc.repository

import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.kyc.entity.KycVerification
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface KycVerificationRepository : MongoRepository<KycVerification, ObjectId> {
    fun findByUserId(userId: ObjectId): KycVerification?
    fun findAllByStatus(status: KycResponse.Status): List<KycVerification>
}
