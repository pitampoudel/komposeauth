package pitampoudel.komposeauth.phone_otp.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.phone_otp.entity.PhoneOtp

@Repository
interface PhoneOtpRepository : MongoRepository<PhoneOtp, ObjectId> {
    fun findByPhoneNumberOrderByCreatedAtDesc(phoneNumber: String): List<PhoneOtp>
    fun deleteByPhoneNumber(phoneNumber: String)
}