package pitampoudel.komposeauth.user.repository

import pitampoudel.komposeauth.user.entity.PhoneOtp
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PhoneOtpRepository : MongoRepository<PhoneOtp, ObjectId> {
    fun findByPhoneNumberOrderByCreatedAtDesc(phoneNumber: String): List<PhoneOtp>
    fun deleteByPhoneNumber(phoneNumber: String)
}