package pitampoudel.komposeauth.setup.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.security.CryptoService
import pitampoudel.komposeauth.setup.entity.Env
import pitampoudel.komposeauth.setup.repository.EnvRepository
import java.util.concurrent.atomic.AtomicReference

@Service
class EnvService(
    private val repo: EnvRepository,
    private val crypto: CryptoService
) {
    private val cache = AtomicReference<Env?>(null)

    fun getEnv(): Env {
        val cached = cache.get()
        if (cached != null) return cached
        val loadedEncrypted = repo.findById(Env.SINGLETON_ID).orElse(Env())
        val loaded = decryptEnv(loadedEncrypted)
        cache.set(loaded)
        return loaded
    }

    fun save(env: Env): Env {
        // Persist encrypted, cache decrypted
        val toSave = encryptEnv(env.copy(id = Env.SINGLETON_ID))
        val savedEncrypted = repo.save(toSave)
        val saved = decryptEnv(savedEncrypted)
        cache.set(saved)
        return saved
    }

    fun update(block: (Env) -> Env): Env {
        return save(block(getEnv()))
    }

    fun clearCache() {
        cache.set(null)
    }

    private fun encryptEnv(src: Env): Env {
        return src.copy(
            googleAuthClientSecret = crypto.encrypt(src.googleAuthClientSecret),
            googleAuthDesktopClientSecret = crypto.encrypt(src.googleAuthDesktopClientSecret),
            twilioAuthToken = crypto.encrypt(src.twilioAuthToken),
            smtpPassword = crypto.encrypt(src.smtpPassword),
        )
    }

    private fun decryptEnv(src: Env): Env {
        return src.copy(
            googleAuthClientSecret = crypto.decrypt(src.googleAuthClientSecret),
            googleAuthDesktopClientSecret = crypto.decrypt(src.googleAuthDesktopClientSecret),
            twilioAuthToken = crypto.decrypt(src.twilioAuthToken),
            smtpPassword = crypto.decrypt(src.smtpPassword)
        )
    }
}