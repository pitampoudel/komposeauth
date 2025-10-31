package pitampoudel.komposeauth.setup.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.setup.entity.AppConfig
import pitampoudel.komposeauth.setup.repository.AppConfigRepository
import java.util.concurrent.atomic.AtomicReference

@Service
class AppConfigService(
    private val repo: AppConfigRepository
) {
    private val cache = AtomicReference<AppConfig?>(null)

    fun get(): AppConfig {
        val cached = cache.get()
        if (cached != null) return cached
        val loaded = repo.findById(AppConfig.Companion.SINGLETON_ID).orElse(AppConfig())
        cache.set(loaded)
        return loaded
    }

    fun save(appConfig: AppConfig): AppConfig {
        val saved = repo.save(appConfig.copy(id = AppConfig.Companion.SINGLETON_ID))
        cache.set(saved)
        return saved
    }

    fun update(block: (AppConfig) -> AppConfig): AppConfig {
        return save(block(get()))
    }

    fun clearCache() {
        cache.set(null)
    }
}