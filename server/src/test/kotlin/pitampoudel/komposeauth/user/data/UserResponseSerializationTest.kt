package pitampoudel.komposeauth.user.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.core.config.SerializationConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Uses the application's own [Json] bean rather than a copy, so a change to its configuration is
 * caught here instead of on the wire.
 */
class UserResponseSerializationTest {

    private val json: Json = SerializationConfig().kotlinxJson()

    private fun user(roles: List<String>) = UserResponse(
        id = "68b0f0f0f0f0f0f0f0f0f0f0",
        firstName = "Test",
        lastName = "User",
        email = "test@example.com",
        emailVerified = true,
        photoUrl = null,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        phoneNumber = null,
        phoneNumberVerified = false,
        kycVerified = false,
        roles = roles
    )

    /**
     * `encodeDefaults` is off, so an empty list — the property's default — would otherwise be
     * dropped and every role-less user would arrive without the field at all.
     */
    @Test
    fun `roles is present even when the user holds none`() {
        val encoded = json.encodeToString(user(emptyList()))
        val roles = json.parseToJsonElement(encoded).jsonObject["roles"]

        assertTrue(roles != null, "roles must be encoded even when empty, got: $encoded")
        assertTrue(roles.jsonArray.isEmpty())
    }

    @Test
    fun `roles round-trips the roles the user holds`() {
        val encoded = json.encodeToString(user(listOf("ADMIN", "SUPPORT")))

        assertEquals(listOf("ADMIN", "SUPPORT"), json.decodeFromString<UserResponse>(encoded).roles)
    }
}
