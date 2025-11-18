package pitampoudel.komposeauth.jwk.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.jwk.entity.Jwk
import pitampoudel.komposeauth.jwk.repository.JwkRepository
import pitampoudel.komposeauth.jwk.utils.RsaPemUtils
import java.security.KeyPair

@Service
class PersistentKeyService(
    private val jwkRepository: JwkRepository
) {

    private val defaultKid = "spring-boot-jwk"

    fun loadOrCreateKeyPair(): KeyPair {
        val existingJwk = jwkRepository.findByKid(defaultKid)
        if (existingJwk.isPresent) {
            val doc = existingJwk.get()
            val pub = RsaPemUtils.publicKeyFromPem(doc.publicKeyPem)
            val priv = RsaPemUtils.privateKeyFromPem(doc.privateKeyPem)
            return KeyPair(pub, priv)
        }

        // If not found: generate & save
        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val pubPem = RsaPemUtils.toPemPublic(kp.public)
        val privPem = RsaPemUtils.toPemPrivate(kp.private)

        val doc = Jwk(
            kid = defaultKid,
            publicKeyPem = pubPem,
            privateKeyPem = privPem
        )

        jwkRepository.save(doc)
        return kp
    }


    fun currentKid(): String = defaultKid
}
