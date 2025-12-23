package pitampoudel.komposeauth.jwk.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DuplicateKeyException
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.jwk.entity.Jwk
import pitampoudel.komposeauth.jwk.repository.JwkRepository
import pitampoudel.komposeauth.jwk.utils.RsaPemUtils
import java.util.Optional
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.assertThrows

class JwkServiceTest {

    @Test
    fun `loads existing jwk and decrypts private key`() {
        val repo = mock<JwkRepository>()
        val crypto = mock<CryptoService>()

        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val pubPem = RsaPemUtils.toPemPublic(kp.public)
        val privPem = RsaPemUtils.toPemPrivate(kp.private)

        whenever(repo.findByKid("spring-boot-jwk")).thenReturn(
            Optional.of(Jwk(kid = "spring-boot-jwk", publicKeyPem = pubPem, privateKeyPem = "enc:abc"))
        )
        whenever(crypto.isEncrypted("enc:abc")).thenReturn(true)
        whenever(crypto.decrypt("enc:abc")).thenReturn(privPem)

        val service = JwkService(repo, crypto)
        val loaded = service.loadOrCreateKeyPair()

        assertEquals(
            (kp.public as java.security.interfaces.RSAPublicKey).modulus,
            (loaded.public as java.security.interfaces.RSAPublicKey).modulus
        )
        verify(repo, never()).save(any())
        verify(crypto, times(1)).decrypt("enc:abc")
    }

    @Test
    fun `rejects existing jwk when private key is stored in plaintext`() {
        val repo = mock<JwkRepository>()
        val crypto = mock<CryptoService>()

        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val pubPem = RsaPemUtils.toPemPublic(kp.public)
        val privPemPlain = RsaPemUtils.toPemPrivate(kp.private)

        whenever(repo.findByKid("spring-boot-jwk")).thenReturn(
            Optional.of(Jwk(kid = "spring-boot-jwk", publicKeyPem = pubPem, privateKeyPem = privPemPlain))
        )
        whenever(crypto.isEncrypted(privPemPlain)).thenReturn(false)

        val service = JwkService(repo, crypto)
        assertThrows<IllegalStateException> {
            service.loadOrCreateKeyPair()
        }
    }

    @Test
    fun `creates new jwk when missing and saves encrypted private key`() {
        val repo = mock<JwkRepository>()
        val crypto = mock<CryptoService>()

        whenever(repo.findByKid("spring-boot-jwk")).thenReturn(Optional.empty())
        whenever(crypto.encrypt(any())).thenAnswer { inv -> "enc:" + inv.arguments[0] as String }

        val captor = argumentCaptor<Jwk>()

        val service = JwkService(repo, crypto)
        val kp = service.loadOrCreateKeyPair()

        verify(repo, times(1)).save(captor.capture())
        val saved = captor.firstValue

        assertEquals("spring-boot-jwk", saved.kid)
        assertTrue(saved.publicKeyPem.contains("BEGIN PUBLIC KEY"))
        assertTrue(saved.privateKeyPem.startsWith("enc:"))

        // ensure ciphertext differs from plaintext
        val privPlain = RsaPemUtils.toPemPrivate(kp.private)
        assertNotEquals(privPlain, saved.privateKeyPem)
    }

    @Test
    fun `when save races with another instance duplicate key is handled by reloading persisted keys`() {
        val repo = mock<JwkRepository>()
        val crypto = mock<CryptoService>()

        // persisted keypair (the one that should win)
        val persistedKp = RsaPemUtils.generateRsaKeyPair(2048)
        val persistedPubPem = RsaPemUtils.toPemPublic(persistedKp.public)
        val persistedPrivPem = RsaPemUtils.toPemPrivate(persistedKp.private)

        // start with empty on first lookup
        whenever(repo.findByKid("spring-boot-jwk"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(Jwk(kid = "spring-boot-jwk", publicKeyPem = persistedPubPem, privateKeyPem = "enc:persisted")))

        whenever(repo.save(any())).thenThrow(DuplicateKeyException("duplicate kid"))
        whenever(crypto.decrypt("enc:persisted")).thenReturn(persistedPrivPem)
        whenever(crypto.encrypt(any())).thenAnswer { inv -> "enc:" + inv.arguments[0] as String }

        val service = JwkService(repo, crypto)
        val kp = service.loadOrCreateKeyPair()

        // Must return the persisted keypair (not the newly generated one)
        assertEquals(
            (persistedKp.public as java.security.interfaces.RSAPublicKey).modulus,
            (kp.public as java.security.interfaces.RSAPublicKey).modulus
        )
        verify(repo, times(1)).save(any())
    }
}
