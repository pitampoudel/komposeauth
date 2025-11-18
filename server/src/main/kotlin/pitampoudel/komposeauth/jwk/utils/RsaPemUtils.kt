package pitampoudel.komposeauth.jwk.utils

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object RsaPemUtils {

    fun generateRsaKeyPair(keySize: Int = 2048): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(keySize)
        return kpg.generateKeyPair()
    }

    fun toPemPublic(publicKey: PublicKey): String {
        val encoded = Base64.getEncoder().encodeToString(publicKey.encoded)
        return buildString {
            appendLine("-----BEGIN PUBLIC KEY-----")
            appendLine(encoded.chunked(64).joinToString("\n"))
            appendLine("-----END PUBLIC KEY-----")
        }
    }

    fun toPemPrivate(privateKey: PrivateKey): String {
        val encoded = Base64.getEncoder().encodeToString(privateKey.encoded)
        return buildString {
            appendLine("-----BEGIN PRIVATE KEY-----") // PKCS#8
            appendLine(encoded.chunked(64).joinToString("\n"))
            appendLine("-----END PRIVATE KEY-----")
        }
    }

    private fun stripPem(pem: String): ByteArray {
        return pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .let { Base64.getDecoder().decode(it) }
    }

    fun publicKeyFromPem(pem: String): RSAPublicKey {
        val spec = X509EncodedKeySpec(stripPem(pem))
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec) as RSAPublicKey
    }

    fun privateKeyFromPem(pem: String): RSAPrivateKey {
        val spec = PKCS8EncodedKeySpec(stripPem(pem))
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec) as RSAPrivateKey
    }
}
