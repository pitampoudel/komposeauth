package com.vardansoft.komposeauth.core.utils

import com.vardansoft.komposeauth.AssetLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL
import java.util.Base64

object WebAuthnUtils {
    /**
     * Generate the Android WebAuthn origin from a colon-separated SHA-256 fingerprint.
     *
     * Example input:
     *  "AA:FB:8B:33:22:CB:FF:FB:ED:7C:76:B2:EB:2F:CA:18:61:D8:2F:94:24:91:FE:5A:B9:FB:9C:14:20:2D:59:51"
     *
     * Output:
     *  "android:apk-key-hash:qvuLMyLL__vtfHay6y_KGGHYL5Qkkf5aufucFCAtWVE"
     */
    fun generateAndroidOrigin(sha256: String): String {
        val bytes = sha256
            .split(":")
            .map { it.trim().toInt(16).toByte() }
            .toByteArray()

        val base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "android:apk-key-hash:$base64Url"
    }

    private fun fetchAssetLinks(rpBaseUrl: String): List<AssetLink> {
        return try {
            val assetLinksUrl = URL("$rpBaseUrl/.well-known/assetlinks.json")
            Json.decodeFromString<List<AssetLink>>(
                assetLinksUrl.readText()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun webAuthnAllowedOrigins(rpBaseUrl: String): Set<String> {
        val assetLinks: List<AssetLink> = fetchAssetLinks(rpBaseUrl)
        return assetLinks.flatMap { assetLink ->
            assetLink.target["sha256_cert_fingerprints"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.contentOrNull
            }?.map { generateAndroidOrigin(it) }.orEmpty()
        }.toSet()
    }
}
