package pitampoudel.komposeauth.user.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.user.entity.OneTimeToken

@Repository
interface OneTimeTokenRepository : MongoRepository<OneTimeToken, ObjectId> {
    fun findByTokenHashAndPurpose(tokenHash: String, purpose: OneTimeToken.Purpose): OneTimeToken?
}
