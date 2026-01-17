package pitampoudel.komposeauth.jwk.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.config.CacheConfig.Companion.JWK_CACHE
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.jwk.entity.Jwk
import pitampoudel.komposeauth.jwk.repository.JwkRepository
import pitampoudel.komposeauth.jwk.utils.RsaPemUtils
import java.security.KeyPair
import java.util.function.Supplier

@Service
class JwkService(
    private val jwkRepository: JwkRepository,
    private val cryptoService: CryptoService
) {

    private val defaultKid = "spring-boot-jwk"

    @Cacheable(value = [JWK_CACHE], key = "'keyPair'")
    fun loadOrCreateKeyPair(): KeyPair {
        val existing = jwkRepository.findByKid(defaultKid).orElse(null)
        if (existing != null) {
            if (!cryptoService.isEncrypted(existing.privateKeyPem)) {
                throw IllegalStateException(
                    "Refusing to load JWK private key for kid='$defaultKid' because it is not encrypted at rest"
                )
            }
            return KeyPair(
                RsaPemUtils.publicKeyFromPem(existing.publicKeyPem),
                RsaPemUtils.privateKeyFromPem(cryptoService.decrypt(existing.privateKeyPem))
            )
        }

        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val pubPem = RsaPemUtils.toPemPublic(kp.public)
        val privPem = RsaPemUtils.toPemPrivate(kp.private)
        val doc = Jwk(
            kid = defaultKid,
            publicKeyPem = pubPem,
            privateKeyPem = cryptoService.encrypt(privPem)
        )

        return try {
            jwkRepository.save(doc)
            kp
        } catch (e: DuplicateKeyException) {
            val persisted = jwkRepository.findByKid(defaultKid)
                .orElseThrow(Supplier { e })
            KeyPair(
                RsaPemUtils.publicKeyFromPem(persisted.publicKeyPem),
                RsaPemUtils.privateKeyFromPem(cryptoService.decrypt(persisted.privateKeyPem))
            )
        }
    }

    fun currentKid(): String = defaultKid
}
