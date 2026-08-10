package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Component
import pitampoudel.komposeauth.StaticAppProperties
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component("masterKeyValidator")
class MasterKeyValidator(val staticAppProperties: StaticAppProperties) {
    fun isValid(masterKey: String?): Boolean {
        if (masterKey.isNullOrEmpty()) return false
        // `+` survives as a space through some form/query decodings, so restore it before comparing.
        val candidate = masterKey.replace(" ", "+")
        // Compare digests, not the keys themselves. `MessageDigest.isEqual` is constant-time only
        // across arrays of equal length — given two different lengths it returns false immediately,
        // which hands back the length of the real key to anyone timing the responses. Digests are
        // always 32 bytes, so every comparison takes the same path whatever was submitted.
        return MessageDigest.isEqual(
            sha256(candidate),
            sha256(staticAppProperties.base64EncryptionKey)
        )
    }

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
}
