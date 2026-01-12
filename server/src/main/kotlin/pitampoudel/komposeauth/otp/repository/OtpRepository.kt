package pitampoudel.komposeauth.otp.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.otp.entity.Otp

@Repository
interface OtpRepository : MongoRepository<Otp, ObjectId> {
    fun findByReceiverOrderByCreatedAtDesc(receiver: String): List<Otp>
    fun deleteByReceiver(receiver: String)
}