package com.vardansoft.auth.user.repository

import com.vardansoft.auth.user.entity.PhoneOtp
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PhoneOtpRepository : MongoRepository<PhoneOtp, ObjectId> {
    fun findByUserIdOrderByCreatedAtDesc(userId: ObjectId): List<PhoneOtp>
    fun deleteByUserId(userId: ObjectId)
}