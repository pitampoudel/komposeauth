package pitampoudel.komposeauth.jwk.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class RsaPemUtilsTest {

    @Test
    fun `public key pem roundtrip preserves modulus and exponent`() {
        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val pub = kp.public as RSAPublicKey

        val pem = RsaPemUtils.toPemPublic(pub)
        val parsed = RsaPemUtils.publicKeyFromPem(pem)

        assertEquals(pub.modulus, parsed.modulus)
        assertEquals(pub.publicExponent, parsed.publicExponent)
    }

    @Test
    fun `private key pem roundtrip can sign and verify`() {
        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val publicKey = kp.public as RSAPublicKey
        val privateKey = kp.private as RSAPrivateKey

        val privPem = RsaPemUtils.toPemPrivate(privateKey)
        val parsedPriv = RsaPemUtils.privateKeyFromPem(privPem)

        val message = "hello".toByteArray()

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(parsedPriv)
        signer.update(message)
        val sig = signer.sign()

        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update(message)
        assertEquals(true, verifier.verify(sig))
    }

    @Test
    fun `pem parsing tolerates whitespace`() {
        val kp = RsaPemUtils.generateRsaKeyPair(2048)
        val pem = RsaPemUtils.toPemPublic(kp.public)

        val noisy = "  \n\n${pem.replace("\n", "\n \t")}\n\n  "
        val parsed = RsaPemUtils.publicKeyFromPem(noisy)

        assertNotNull(parsed)
    }

    @Test
    fun `invalid pem throws`() {
        val bad = "-----BEGIN PUBLIC KEY-----\nnot-base64!!\n-----END PUBLIC KEY-----"
        assertThrows(IllegalArgumentException::class.java) {
            RsaPemUtils.publicKeyFromPem(bad)
        }
    }
}
