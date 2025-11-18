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
            googleAuthClientSecret = src.googleAuthClientSecret?.let { crypto.encrypt(it) },
            googleAuthDesktopClientSecret = src.googleAuthDesktopClientSecret?.let {
                crypto.encrypt(it)
            },
            twilioAuthToken = src.twilioAuthToken?.let { crypto.encrypt(it) },
            smtpPassword = src.smtpPassword?.let { crypto.encrypt(it) },
        )
    }

    private fun decryptEnv(src: Env): Env {
        return src.copy(
            googleAuthClientSecret = src.googleAuthClientSecret?.let { crypto.decrypt(it) },
            googleAuthDesktopClientSecret = src.googleAuthDesktopClientSecret?.let {
                crypto.decrypt(it)
            },
            twilioAuthToken = src.twilioAuthToken?.let { crypto.decrypt(it) },
            smtpPassword = src.smtpPassword?.let { crypto.decrypt(it) }
        )
    }
}