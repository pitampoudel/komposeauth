package pitampoudel.komposeauth.webauthn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * A platform authenticator in software: one ES256 key, "none" attestation, user present and
 * verified. It answers the options exactly as a phone's passkey provider would, so the server's
 * verification runs against real signatures rather than against a stub.
 */
class TestAuthenticator(private val origin: String) {
    private val keyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
    val credentialId: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
    private var userHandle: String? = null
    private var signCount = 0

    /** The `credential` a client posts after `navigator.credentials.create(options)`, as Android shapes it. */
    fun create(optionsJson: String): JsonObject {
        val options = Json.parseToJsonElement(optionsJson).jsonObject
        val rpId = options.getValue("rp").jsonObject.getValue("id").jsonPrimitive.content
        userHandle = options.getValue("user").jsonObject.getValue("id").jsonPrimitive.content
        val clientData = clientData("webauthn.create", options.getValue("challenge").jsonPrimitive.content)

        val authData = ByteArrayOutputStream().apply {
            write(sha256(rpId.toByteArray()))
            write(FLAG_USER_PRESENT or FLAG_USER_VERIFIED or FLAG_ATTESTED_CREDENTIAL)
            write(uint32(signCount))
            write(ByteArray(16))
            write(byteArrayOf((credentialId.size shr 8).toByte(), credentialId.size.toByte()))
            write(credentialId)
            write(coseKey())
        }.toByteArray()
        val attestationObject = ByteArrayOutputStream().apply {
            write(0xA3)
            write(cborText("fmt")); write(cborText("none"))
            write(cborText("attStmt")); write(0xA0)
            write(cborText("authData")); write(cborBytes(authData))
        }.toByteArray()

        return credential {
            putJsonObject("response") {
                put("attestationObject", base64Url(attestationObject))
                put("clientDataJSON", base64Url(clientData))
                put("transports", buildJsonArray { add("internal") })
                // Android and the browsers' toJSON() send these too, and the server reads past them.
                put("authenticatorData", base64Url(authData))
                put("publicKeyAlgorithm", -7)
                put("publicKey", base64Url(keyPair.public.encoded))
            }
            putJsonObject("clientExtensionResults") { putJsonObject("credProps") { put("rk", true) } }
        }
    }

    /** What a client posts after `navigator.credentials.get(options)`, as a JSON string. */
    fun get(optionsJson: String, origin: String = this.origin): String {
        val options = Json.parseToJsonElement(optionsJson).jsonObject
        val rpId = options.getValue("rpId").jsonPrimitive.content
        val clientData = clientData("webauthn.get", options.getValue("challenge").jsonPrimitive.content, origin)
        signCount++
        val authData = sha256(rpId.toByteArray()) +
                byteArrayOf((FLAG_USER_PRESENT or FLAG_USER_VERIFIED).toByte()) +
                uint32(signCount)
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(authData + sha256(clientData))
            sign()
        }
        return credential {
            putJsonObject("response") {
                put("authenticatorData", base64Url(authData))
                put("clientDataJSON", base64Url(clientData))
                put("signature", base64Url(signature))
                put("userHandle", userHandle)
            }
            putJsonObject("clientExtensionResults") {}
        }.toString()
    }

    private fun credential(body: JsonObjectBuilder.() -> Unit) = buildJsonObject {
        put("id", base64Url(credentialId))
        put("rawId", base64Url(credentialId))
        put("type", "public-key")
        put("authenticatorAttachment", "platform")
        body()
    }

    private fun clientData(type: String, challenge: String, origin: String = this.origin): ByteArray =
        """{"type":"$type","challenge":"$challenge","origin":"$origin","crossOrigin":false}""".toByteArray()

    /** The public key as COSE: EC2, ES256, P-256, then the two coordinates. */
    private fun coseKey(): ByteArray {
        val point = (keyPair.public as ECPublicKey).w
        return byteArrayOf(0xA5.toByte(), 0x01, 0x02, 0x03, 0x26, 0x20, 0x01) +
                byteArrayOf(0x21) + cborBytes(coordinate(point.affineX)) +
                byteArrayOf(0x22) + cborBytes(coordinate(point.affineY))
    }

    private fun coordinate(value: BigInteger): ByteArray =
        value.toByteArray().takeLast(32).toByteArray().let { ByteArray(32 - it.size) + it }

    private fun cborText(value: String): ByteArray = cborHeader(3, value.length) + value.toByteArray()

    private fun cborBytes(value: ByteArray): ByteArray = cborHeader(2, value.size) + value

    private fun cborHeader(majorType: Int, length: Int): ByteArray {
        val type = majorType shl 5
        return when {
            length < 24 -> byteArrayOf((type or length).toByte())
            length < 256 -> byteArrayOf((type or 24).toByte(), length.toByte())
            else -> byteArrayOf((type or 25).toByte(), (length shr 8).toByte(), length.toByte())
        }
    }

    private fun uint32(value: Int) =
        byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private companion object {
        const val FLAG_USER_PRESENT = 0x01
        const val FLAG_USER_VERIFIED = 0x04
        const val FLAG_ATTESTED_CREDENTIAL = 0x40
    }
}
