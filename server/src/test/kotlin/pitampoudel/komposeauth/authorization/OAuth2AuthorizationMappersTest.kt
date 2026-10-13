package pitampoudel.komposeauth.authorization

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OAuth2AuthorizationMappersTest {

    @Test
    fun `fixOidcClaimTypes should convert standard claims to Instant when loading from database`() {
        val nowSeconds = Instant.now().epochSecond
        val claims = mutableMapOf<String, Any>(
            "iat" to nowSeconds,
            "exp" to nowSeconds + 3600,
            "updated_at" to 123456789L
        )

        val fixed = fixOidcClaimTypes(claims)

        // Verifying that they are converted to Instant.
        // Spring Security standard getters (OidcIdToken, OidcUserInfo) expect java.time.Instant
        // for these specific claims and will throw ClassCastException if they find a Long.
        assertTrue(fixed["iat"] is Instant, "iat should be Instant, but was ${fixed["iat"]?.javaClass?.name}")
        assertEquals(nowSeconds, (fixed["iat"] as Instant).epochSecond)
        assertTrue(fixed["exp"] is Instant, "exp should be Instant, but was ${fixed["exp"]?.javaClass?.name}")
        assertEquals(nowSeconds + 3600, (fixed["exp"] as Instant).epochSecond)
        assertTrue(fixed["updated_at"] is Instant)
        assertEquals(123456789L, (fixed["updated_at"] as Instant).epochSecond)
    }

    @Test
    fun `fixOidcClaimTypes should leave Instant claims unchanged`() {
        val now = Instant.now()
        val claims = mutableMapOf<String, Any>(
            "iat" to now,
            "exp" to now.plusSeconds(3600),
            "custom" to "value"
        )

        val fixed = fixOidcClaimTypes(claims)

        assertTrue(fixed["iat"] is Instant)
        assertEquals(now, fixed["iat"])
        assertTrue(fixed["exp"] is Instant)
        assertEquals(now.plusSeconds(3600), fixed["exp"])
        assertEquals("value", fixed["custom"])
    }

    @Test
    fun `serializeOidcClaimTypes should convert Instant claims to epoch seconds`() {
        val now = Instant.now()
        val claims = mutableMapOf<String, Any>(
            "iat" to now,
            "exp" to now.plusSeconds(3600),
            "custom" to "value"
        )

        val fixed = serializeOidcClaimTypes(claims)

        assertTrue(fixed["iat"] is Long)
        assertEquals(now.epochSecond, fixed["iat"])
        assertTrue(fixed["exp"] is Long)
        assertEquals(now.plusSeconds(3600).epochSecond, fixed["exp"])
        assertEquals("value", fixed["custom"])
    }

    @Test
    fun `fixOidcClaimTypes should convert double timestamp claims to Instant`() {
        val claims = mutableMapOf<String, Any>(
            "iat" to 123456789.0,
            "updated_at" to 123456790.0,
            "customNumber" to 42.0
        )

        val fixed = fixOidcClaimTypes(claims)

        assertTrue(fixed["iat"] is Instant)
        assertEquals(123456789L, (fixed["iat"] as Instant).epochSecond)
        assertTrue(fixed["updated_at"] is Instant)
        assertEquals(123456790L, (fixed["updated_at"] as Instant).epochSecond)
        assertEquals(42.0, fixed["customNumber"])
    }

    @Test
    fun `fixOidcClaimTypes should handle nested maps and convert to Instant`() {
        val nowSeconds = Instant.now().epochSecond
        val claims = mutableMapOf<String, Any>(
            "nested" to mutableMapOf<String, Any>(
                "iat" to nowSeconds
            )
        )

        val fixed = fixOidcClaimTypes(claims)
        val nested = fixed["nested"] as Map<*, *>
        assertTrue(nested["iat"] is Instant)
        assertEquals(nowSeconds, (nested["iat"] as Instant).epochSecond)
    }

    @Test
    fun `a revoked refresh token is still revoked when read back`() {
        val client = RegisteredClient.withId("c1")
            .clientId("c1")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .build()
        val now = Instant.now()
        val issued = OAuth2Authorization.withRegisteredClient(client)
            .principalName("user")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .accessToken(OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "a1", now, now.plusSeconds(900)))
            .refreshToken(OAuth2RefreshToken("r1", now, now.plusSeconds(86_400)))
            .build()
        val revoked = OAuth2Authorization.from(issued).invalidate(issued.refreshToken!!.token).build()
        val objectMapper = ObjectMapper()

        val read = assertNotNull(
            fromOAuth2AuthorizationDocument(toOAuth2AuthorizationDocument(revoked, objectMapper), client, objectMapper)
        )

        assertTrue(read.refreshToken!!.isInvalidated)
        assertFalse(read.refreshToken!!.isActive)
        assertTrue(read.accessToken!!.isInvalidated)
    }
}
