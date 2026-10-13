package pitampoudel.komposeauth.kyc.repository

import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.FindAndReplaceOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface KycVerificationRepository : MongoRepository<KycVerification, ObjectId>, KycVerificationRepositoryCustom {
    fun findByUserId(userId: ObjectId): KycVerification?
    fun findAllByStatus(status: KycResponse.Status): List<KycVerification>
}

interface KycVerificationRepositoryCustom {
    /**
     * Stores [replacement] only while the stored record still has the [expected] status, in one
     * step, and returns it; null when another request moved the status on first (or there is none).
     */
    fun replaceIfStatus(expected: KycResponse.Status, replacement: KycVerification): KycVerification?
}

class KycVerificationRepositoryImpl(
    private val mongoTemplate: MongoTemplate
) : KycVerificationRepositoryCustom {
    override fun replaceIfStatus(expected: KycResponse.Status, replacement: KycVerification): KycVerification? =
        mongoTemplate.findAndReplace(
            Query(Criteria.where("_id").`is`(replacement.userId).and("status").`is`(expected)),
            replacement,
            FindAndReplaceOptions.options().returnNew()
        )
}
