package pitampoudel.komposeauth.core.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for application-level caching to improve performance.
 * Uses in-memory caching with Spring's Cache abstraction.
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val USERS_CACHE = "users"
        const val OAUTH2_CLIENTS_CACHE = "oauth2Clients"
        const val JWK_CACHE = "jwk"
        const val ORGANIZATIONS_CACHE = "organizations"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                ConcurrentMapCache(USERS_CACHE),
                ConcurrentMapCache(OAUTH2_CLIENTS_CACHE),
                ConcurrentMapCache(JWK_CACHE),
                ConcurrentMapCache(ORGANIZATIONS_CACHE)
            )
        )
        return cacheManager
    }
}
