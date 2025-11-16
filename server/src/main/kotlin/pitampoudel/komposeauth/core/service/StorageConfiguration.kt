package pitampoudel.komposeauth.core.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.AppProperties

@Configuration
class StorageConfiguration {

    /**
     * Always provide a local storage service for sensitive data (e.g., private keys).
     */
    @Bean(name = ["localStorageService", "secureStorageService"])
    fun localStorageService(appProperties: AppProperties): StorageService {
        return LocalStorageService(appProperties)
    }

    /**
     * Provide a GCP storage service when GCP bucket is configured; otherwise, don't register it.
     */
    @Bean(name = ["gcpStorageService"], autowireCandidate = true)
    fun gcpStorageService(appProperties: AppProperties): StorageService? {
        return if (!appProperties.gcpBucketName.isNullOrBlank()) {
            GcpStorageService(appProperties)
        } else {
            null
        }
    }

    /**
     * Media storage prefers GCP when available; otherwise falls back to local.
     */
    @Bean(name = ["mediaStorageService"])
    fun mediaStorageService(
        secureStorageService: StorageService,
        gcpStorageService: StorageService?
    ): StorageService {
        return gcpStorageService ?: secureStorageService
    }
}