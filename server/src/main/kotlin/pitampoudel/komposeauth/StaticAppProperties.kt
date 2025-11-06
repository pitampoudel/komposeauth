package pitampoudel.komposeauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.Base64

@Configuration
@ConfigurationProperties(prefix = "app")
class StaticAppProperties(private val env: Environment) {
    var base64EncryptionKey: String? = null
        get() {
            if (!field.isNullOrBlank()) return field
            // If running with the 'test' profile and no key provided, use a deterministic zero-key
            val isTest = env.activeProfiles.any { it.equals("test", ignoreCase = true) }
            if (isTest) {
                return Base64.getEncoder().encodeToString(ByteArray(32))
            }
            return null
        }
}
