package pitampoudel.komposeauth.app_config.utils

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val AES_KEY_BYTES = 32 // 256-bit
private const val GCM_TAG_BITS = 128
private const val IV_BYTES = 12

object Crypto {
    data class EncryptedString(
        val cipherText: String,
        val iv: String
    )

    private val secureRandom = SecureRandom()

    fun keyFromString(key: String): SecretKey {
        // Expect 32-byte key material. If shorter, right-pad; if longer, cut.
        val keyBytes = key.encodeToByteArray().copyOf(AES_KEY_BYTES)
        return SecretKeySpec(keyBytes, "AES")
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(plain: String, key: SecretKey): EncryptedString {
        val iv = ByteArray(IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plain.encodeToByteArray())
        return EncryptedString(
            cipherText = Base64.encode(cipherText),
            iv = Base64.encode(iv),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(enc: EncryptedString, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(enc.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = Base64.decode(enc.cipherText)
        val plainBytes = cipher.doFinal(cipherBytes)
        return plainBytes.decodeToString()
    }
}
