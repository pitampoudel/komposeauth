package pitampoudel.komposeauth.core.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.AppProperties

@Configuration
class StorageConfiguration {

    @Bean
    fun storageService(appProperties: AppProperties): StorageService {
        return if (!appProperties.gcpBucketName.isNullOrBlank()) {
            GcpStorageService(appProperties)
        } else {
            LocalStorageService(appProperties)
        }
    }
}