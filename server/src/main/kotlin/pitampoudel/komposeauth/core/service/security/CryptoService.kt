package pitampoudel.komposeauth.core.service.security

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.StaticAppProperties
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Simple AES-GCM encryption service for protecting sensitive configuration values and files at rest.
 * A string's ciphertext format is: "enc:" + Base64( IV(12) || CIPHERTEXT+TAG ); a file's is [seal]'s.
 */
@Service
class CryptoService(private val props: StaticAppProperties) {

    init {
        // Validate Base64 and size early to fail fast
        resolveKey()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES = "AES"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        private const val PREFIX = "enc:"

        /** Readable on purpose: somebody who opens a sealed file in the bucket sees what it is. */
        private val SEALED_HEADER = "komposeauth-sealed/1\n".toByteArray(Charsets.US_ASCII)
    }

    private val secureRandom = SecureRandom()

    private fun resolveKey(): SecretKey {
        val keyString = props.base64EncryptionKey
        if (keyString.isBlank()) {
            throw IllegalStateException("encryption key must be provided")
        }
        val raw = try {
            Base64.getDecoder().decode(keyString)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("encryption key must be Base64-encoded", e)
        }
        if (raw.size != 16 && raw.size != 24 && raw.size != 32) {
            throw IllegalStateException("encryption key must be 128, 192, or 256 bits (Base64-encoded)")
        }
        return SecretKeySpec(raw, AES)
    }

    fun isEncrypted(value: String?): Boolean = value?.startsWith(PREFIX) == true

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        if (isEncrypted(plain)) return plain // idempotent

        val key = resolveKey()
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        val payload = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(ciphertext, 0, payload, iv.size, ciphertext.size)
        val encoded = Base64.getEncoder().encodeToString(payload)
        return PREFIX + encoded
    }

    fun decrypt(value: String): String {
        if (value.isEmpty()) return value
        if (!isEncrypted(value)) return value // backward compatibility

        val encoded = value.removePrefix(PREFIX)
        val payload = try {
            Base64.getDecoder().decode(encoded)
        } catch (e: IllegalArgumentException) {
            // Not a valid ciphertext, return as-is to avoid data loss
            return value
        }
        if (payload.size <= IV_LENGTH_BYTES) return value

        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val encrypted = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

        val key = resolveKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val plainBytes = cipher.doFinal(encrypted)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun isSealed(bytes: ByteArray): Boolean =
        bytes.size > SEALED_HEADER.size + IV_LENGTH_BYTES &&
            bytes.copyOfRange(0, SEALED_HEADER.size).contentEquals(SEALED_HEADER)

    /**
     * [plain] encrypted under the same key, in a form a file can be stored in: a header, the IV, then
     * the ciphertext and tag. [context] is bound in as associated data, so a sealed file opens only
     * under the context it was sealed for and cannot be passed off as another.
     */
    fun seal(plain: ByteArray, context: String): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, resolveKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.updateAAD(context.toByteArray(Charsets.UTF_8))
        return SEALED_HEADER + iv + cipher.doFinal(plain)
    }

    /** What [seal] sealed under [context]. Throws for anything else, or for a file that was altered. */
    fun open(sealed: ByteArray, context: String): ByteArray {
        require(isSealed(sealed)) { "Not a sealed file" }
        val ivEnd = SEALED_HEADER.size + IV_LENGTH_BYTES
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            resolveKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, sealed.copyOfRange(SEALED_HEADER.size, ivEnd))
        )
        cipher.updateAAD(context.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(sealed, ivEnd, sealed.size - ivEnd)
    }
}
