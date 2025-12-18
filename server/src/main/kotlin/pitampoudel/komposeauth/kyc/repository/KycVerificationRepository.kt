package pitampoudel.komposeauth.kyc.repository

import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface KycVerificationRepository : MongoRepository<KycVerification, ObjectId> {
    fun findByUserId(userId: ObjectId): KycVerification?
    fun findAllByStatus(status: KycResponse.Status): List<KycVerification>
}
