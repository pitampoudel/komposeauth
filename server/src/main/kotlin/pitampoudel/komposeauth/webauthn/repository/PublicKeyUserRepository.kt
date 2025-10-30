package pitampoudel.komposeauth.webauthn.repository

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser

@Repository
interface PublicKeyUserRepository : MongoRepository<PublicKeyUser, String> {
    fun findByName(name: String): PublicKeyUser?
}
