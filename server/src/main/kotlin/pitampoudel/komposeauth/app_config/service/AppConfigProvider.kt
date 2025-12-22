package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.repository.AppConfigRepository
import java.util.concurrent.atomic.AtomicReference

@Service
class AppConfigProvider(
    private val repo: AppConfigRepository,
    private val crypto: CryptoService
) {
    private val cache = AtomicReference<AppConfig?>(null)

    fun get(): AppConfig {
        val cached = cache.get()
        if (cached != null) return cached
        val loadedEncrypted = repo.findById(AppConfig.SINGLETON_ID).orElse(AppConfig()).clean()
        val loaded = decrypt(loadedEncrypted)
        cache.set(loaded)
        return loaded
    }

    fun save(appConfig: AppConfig): AppConfig {
        // Persist encrypted, cache decrypted
        val toSave = encrypt(appConfig.copy(id = AppConfig.SINGLETON_ID).clean())
        val savedEncrypted = repo.save(toSave)
        val saved = decrypt(savedEncrypted)
        cache.set(saved)
        return saved
    }

    fun clearCache() {
        cache.set(null)
    }

    private fun encrypt(src: AppConfig): AppConfig {
        return src.copy(
            googleAuthClientSecret = src.googleAuthClientSecret?.let { crypto.encrypt(it) },
            googleAuthDesktopClientSecret = src.googleAuthDesktopClientSecret?.let {
                crypto.encrypt(it)
            },
            twilioAuthToken = src.twilioAuthToken?.let { crypto.encrypt(it) },
            smtpPassword = src.smtpPassword?.let { crypto.encrypt(it) },
            samayeApiKey = src.samayeApiKey?.let { crypto.encrypt(it) },
        )
    }

    private fun decrypt(src: AppConfig): AppConfig {
        return src.copy(
            googleAuthClientSecret = src.googleAuthClientSecret?.let { crypto.decrypt(it) },
            googleAuthDesktopClientSecret = src.googleAuthDesktopClientSecret?.let {
                crypto.decrypt(it)
            },
            twilioAuthToken = src.twilioAuthToken?.let { crypto.decrypt(it) },
            smtpPassword = src.smtpPassword?.let { crypto.decrypt(it) },
            samayeApiKey = src.samayeApiKey?.let { crypto.decrypt(it) }
        )
    }
}