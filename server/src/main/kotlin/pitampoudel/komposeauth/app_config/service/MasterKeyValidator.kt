package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Component
import pitampoudel.komposeauth.StaticAppProperties
import java.security.MessageDigest
import java.nio.charset.StandardCharsets

@Component("masterKeyValidator")
class MasterKeyValidator(val staticAppProperties: StaticAppProperties) {
    fun isValid(masterKey: String?): Boolean {
        if (masterKey.isNullOrEmpty()) return false
        // `+` survives as a space through some form/query decodings, so restore it before comparing.
        val candidate = masterKey.replace(" ", "+")
        // Constant-time: a plain `==` returns as soon as two characters differ, which leaks the
        // shared secret one character at a time to anyone who can time the responses.
        return MessageDigest.isEqual(
            candidate.toByteArray(StandardCharsets.UTF_8),
            staticAppProperties.base64EncryptionKey.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
