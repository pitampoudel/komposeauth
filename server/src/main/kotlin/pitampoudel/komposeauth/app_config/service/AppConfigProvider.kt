package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.repository.AppConfigRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Service
class AppConfigProvider(
    private val repo: AppConfigRepository,
    private val crypto: CryptoService
) {
    private data class Cached(val value: AppConfig, val loadedAt: Instant)

    private val cache = AtomicReference<Cached?>(null)

    fun get(): AppConfig {
        val cached = cache.get()
        if (cached != null && !isStale(cached)) return cached.value
        val loadedEncrypted = repo.findById(AppConfig.SINGLETON_ID).orElse(AppConfig()).clean()
        val loaded = decrypt(loadedEncrypted)
        cache.set(Cached(loaded, Instant.now()))
        return loaded
    }

    fun save(appConfig: AppConfig): AppConfig {
        // Persist encrypted, cache decrypted
        val toSave = encrypt(appConfig.copy(id = AppConfig.SINGLETON_ID).clean())
        val savedEncrypted = repo.save(toSave)
        val saved = decrypt(savedEncrypted)
        cache.set(Cached(saved, Instant.now()))
        return saved
    }

    fun clearCache() {
        cache.set(null)
    }

    /**
     * The cache lives in one process, so a save only refreshes the instance that handled it. Held
     * forever, every other instance would keep serving the old configuration until it happened to
     * restart — which matters here because this config carries the CORS origin list and the
     * provider secrets, so narrowing access or rotating a leaked key would silently not take effect
     * fleet-wide. A short expiry bounds that to [CACHE_TTL] without giving up the read caching.
     */
    private fun isStale(cached: Cached): Boolean =
        Duration.between(cached.loadedAt, Instant.now()) >= CACHE_TTL

    private companion object {
        val CACHE_TTL: Duration = Duration.ofSeconds(60)
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
            thirdFactorSecretKey = src.thirdFactorSecretKey?.let { crypto.encrypt(it) },
            thirdFactorToken = src.thirdFactorToken?.let { crypto.encrypt(it) },
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
            samayeApiKey = src.samayeApiKey?.let { crypto.decrypt(it) },
            thirdFactorSecretKey = src.thirdFactorSecretKey?.let { crypto.decrypt(it) },
            thirdFactorToken = src.thirdFactorToken?.let { crypto.decrypt(it) },
        )
    }
}