package pitampoudel.komposeauth.jwk.repository

import org.springframework.data.mongodb.repository.MongoRepository
import pitampoudel.komposeauth.jwk.entity.Jwk
import java.util.Optional

interface JwkRepository : MongoRepository<Jwk, String> {
    fun findByKid(kid: String): Optional<Jwk>
}
