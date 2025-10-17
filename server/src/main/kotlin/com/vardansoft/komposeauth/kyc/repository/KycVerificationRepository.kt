package com.vardansoft.komposeauth.kyc.repository

import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.komposeauth.kyc.entity.KycVerification
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface KycVerificationRepository : MongoRepository<KycVerification, ObjectId> {
    fun findByUserId(userId: ObjectId): KycVerification?
    fun findAllByStatus(status: KycResponse.Status): List<KycVerification>
}
