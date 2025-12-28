package pitampoudel.komposeauth.core.service.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pitampoudel.komposeauth.StaticAppProperties
import java.util.Base64

class CryptoServiceTest {

    private fun propsWithKey(base64Key: String): StaticAppProperties = StaticAppProperties().apply {
        base64EncryptionKey = base64Key
    }

    private fun base64KeyOfLength(bytes: Int): String {
        val raw = ByteArray(bytes) { i -> (i + 1).toByte() }
        return Base64.getEncoder().encodeToString(raw)
    }

    @Test
    fun `rejects non-base64 encryption key`() {
        assertThrows<IllegalStateException> {
            CryptoService(propsWithKey("not-base64!!!"))
        }
    }

    @Test
    fun `rejects invalid key length`() {
        val bad = base64KeyOfLength(15)
        assertThrows<IllegalStateException> {
            CryptoService(propsWithKey(bad))
        }
    }

    @Test
    fun `rejects missing encryption key`() {
        val props = StaticAppProperties()
        assertThrows<UninitializedPropertyAccessException> {
            CryptoService(props)
        }
    }

    @Test
    fun `encrypt decrypt round trip works`() {
        val service = CryptoService(propsWithKey(base64KeyOfLength(16)))
        val plain = "hello world"
        val enc = service.encrypt(plain)
        assertNotEquals(plain, enc)
        assertTrue(enc.startsWith("enc:"))
        assertEquals(plain, service.decrypt(enc))
    }

    @Test
    fun `encrypt is idempotent for already encrypted values`() {
        val service = CryptoService(propsWithKey(base64KeyOfLength(16)))
        val enc = service.encrypt("secret")
        assertEquals(enc, service.encrypt(enc))
    }

    @Test
    fun `encrypt uses random IV so ciphertext differs for same plaintext`() {
        val service = CryptoService(propsWithKey(base64KeyOfLength(16)))
        val a = service.encrypt("same")
        val b = service.encrypt("same")
        assertNotEquals(a, b)
        assertEquals("same", service.decrypt(a))
        assertEquals("same", service.decrypt(b))
    }

    @Test
    fun `decrypt returns original value for non encrypted input`() {
        val service = CryptoService(propsWithKey(base64KeyOfLength(16)))
        assertEquals("plain", service.decrypt("plain"))
    }

    @Test
    fun `decrypt returns original value for invalid ciphertext`() {
        val service = CryptoService(propsWithKey(base64KeyOfLength(16)))
        assertEquals("enc:%%%", service.decrypt("enc:%%%"))
    }

    @Test
    fun `empty values are preserved`() {
        val service = CryptoService(propsWithKey(base64KeyOfLength(16)))
        assertEquals("", service.encrypt(""))
        assertEquals("", service.decrypt(""))
    }
}
