package pitampoudel.komposeauth.webauthn.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser

@Repository
interface PublicKeyUserRepository : MongoRepository<PublicKeyUser, ObjectId> {
    fun findByName(name: String): PublicKeyUser?
    fun findByUserId(userId: ObjectId): PublicKeyUser?
    fun findByUserHandle(userHandle: Bytes): PublicKeyUser?
}
