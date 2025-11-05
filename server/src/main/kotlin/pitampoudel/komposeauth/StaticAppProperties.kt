package pitampoudel.komposeauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app")
class StaticAppProperties() {
    lateinit var base64EncryptionKey: String
}
