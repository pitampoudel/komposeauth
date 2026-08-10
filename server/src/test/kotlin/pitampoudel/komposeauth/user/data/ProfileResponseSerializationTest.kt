package pitampoudel.komposeauth.user.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.core.config.SerializationConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Uses the application's own [Json] bean rather than a copy, so a change to its configuration is
 * caught here instead of on the wire.
 */
class ProfileResponseSerializationTest {

    private val json: Json = SerializationConfig().kotlinxJson()

    private fun profile(kycVerified: Boolean) = ProfileResponse(
        givenName = "Test",
        familyName = "User",
        email = "test@example.com",
        phoneNumber = null,
        emailVerified = true,
        phoneNumberVerified = false,
        kycVerified = kycVerified,
        picture = null,
        id = "68b0f0f0f0f0f0f0f0f0f0f0",
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        socialLinks = emptyList(),
        roles = emptyList()
    )

    /**
     * `encodeDefaults` is off, so `false` — the property's default — would otherwise be dropped and
     * a client could not tell an unverified user apart from a server that said nothing.
     */
    @Test
    fun `kycVerified is present even when the user is not verified`() {
        val encoded = json.encodeToString(profile(kycVerified = false))
        val kycVerified = json.parseToJsonElement(encoded).jsonObject["kycVerified"]

        assertTrue(kycVerified != null, "kycVerified must be encoded even when false, got: $encoded")
        assertEquals("false", kycVerified.jsonPrimitive.content)
    }

    @Test
    fun `kycVerified round-trips a verified user`() {
        val encoded = json.encodeToString(profile(kycVerified = true))

        assertEquals(true, json.decodeFromString<ProfileResponse>(encoded).kycVerified)
    }
}
