package pitampoudel.komposeauth.authorization

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OAuth2AuthorizationMappersTest {

    @Test
    fun `fixOidcClaimTypes should convert Instant to Long for standard claims`() {
        val now = Instant.now()
        val claims = mutableMapOf<String, Any>(
            "iat" to now,
            "exp" to now.plusSeconds(3600),
            "custom" to "value"
        )

        val fixed = fixOidcClaimTypes(claims)

        assertTrue(fixed["iat"] is Long)
        assertEquals(now.epochSecond, fixed["iat"])
        assertTrue(fixed["exp"] is Long)
        assertEquals(now.plusSeconds(3600).epochSecond, fixed["exp"])
        assertEquals("value", fixed["custom"])
    }

    @Test
    fun `fixOidcClaimTypes should keep standard claims as Long when loading from database`() {
        val nowSeconds = Instant.now().epochSecond
        val claims = mutableMapOf<String, Any>(
            "iat" to nowSeconds,
            "exp" to nowSeconds + 3600,
            "updated_at" to 123456789L
        )

        val fixed = fixOidcClaimTypes(claims)

        // Verifying that they are kept as Long (Number) and NOT converted to Instant
        // because Nimbus's shaded Gson fails to serialize java.time.Instant on Java 17+.
        // Spring Security getters (OidcIdToken, Jwt) handle Long, Date, or Instant automatically.
        assertTrue(fixed["iat"] is Long, "iat should be Long, but was ${fixed["iat"]?.javaClass?.name}")
        assertEquals(nowSeconds, fixed["iat"])
        assertTrue(fixed["exp"] is Long, "exp should be Long, but was ${fixed["exp"]?.javaClass?.name}")
        assertEquals(nowSeconds + 3600, fixed["exp"])
        assertTrue(fixed["updated_at"] is Long)
        assertEquals(123456789L, fixed["updated_at"])
    }

    @Test
    fun `fixOidcClaimTypes should handle nested maps`() {
        val now = Instant.now()
        val claims = mutableMapOf<String, Any>(
            "nested" to mutableMapOf<String, Any>(
                "iat" to now
            )
        )

        val fixed = fixOidcClaimTypes(claims)
        val nested = fixed["nested"] as Map<*, *>
        assertTrue(nested["iat"] is Long)
        assertEquals(now.epochSecond, nested["iat"])
    }
}
