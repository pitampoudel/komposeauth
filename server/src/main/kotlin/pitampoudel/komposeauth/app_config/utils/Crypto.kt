package pitampoudel.komposeauth.app_config.utils

import java.security.MessageDigest
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
private const val MIN_KEY_CHARS = 16

object Crypto {
    data class EncryptedString(
        val cipherText: String,
        val iv: String
    )

    private val secureRandom = SecureRandom()

    /**
     * Derives a 256-bit AES key from [key].
     *
     * Padding short input with zero bytes — the previous behaviour — meant a 6-character passphrase
     * produced a key with 6 bytes of entropy and 26 bytes of nothing, while giving no sign that the
     * key was weak. Hashing spreads whatever entropy the input has across the full key, and the
     * length floor keeps genuinely weak input from being accepted at all.
     */
    fun keyFromString(key: String): SecretKey {
        require(key.length >= MIN_KEY_CHARS) {
            "Encryption key must be at least $MIN_KEY_CHARS characters"
        }
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(key.encodeToByteArray())
        return SecretKeySpec(keyBytes.copyOf(AES_KEY_BYTES), "AES")
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
