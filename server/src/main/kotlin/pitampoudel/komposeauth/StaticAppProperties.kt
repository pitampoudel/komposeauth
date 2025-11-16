package pitampoudel.komposeauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.Base64
import javax.crypto.KeyGenerator

@Configuration
@ConfigurationProperties(prefix = "app")
class StaticAppProperties(private val env: Environment) {
    var base64EncryptionKey: String? = null
        get() {
            if (!field.isNullOrBlank()) return field
            // If running with the 'test' profile and no key provided, generate and cache a key
            val isTest = env.activeProfiles.any { it.equals("test", ignoreCase = true) }
            if (isTest) {
                val generated = generateKey()
                field = generated
                return generated
            }
            throw IllegalStateException("No encryption key found")
        }

    fun generateKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // or 128/192
        val key = keyGen.generateKey()
        return Base64.getEncoder().encodeToString(key.encoded)
    }
}
