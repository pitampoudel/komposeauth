package pitampoudel.komposeauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app")
class StaticAppProperties() {
    var base64EncryptionKey: String? = null
        get() {
            if (!field.isNullOrBlank()) return field
            throw IllegalStateException("No encryption key found")
        }
}
