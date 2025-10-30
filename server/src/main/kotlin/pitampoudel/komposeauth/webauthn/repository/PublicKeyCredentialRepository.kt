package pitampoudel.komposeauth.webauthn.repository

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.webauthn.entity.PublicKeyCredential

@Repository
interface PublicKeyCredentialRepository : MongoRepository<PublicKeyCredential, Bytes> {
    fun findAllByPublicKeyUserId(publicKeyUserId: Bytes): List<PublicKeyCredential>
}