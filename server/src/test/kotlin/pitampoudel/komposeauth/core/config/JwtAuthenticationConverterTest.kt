package pitampoudel.komposeauth.core.config

import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.TestJwtUtil
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtAuthenticationConverterTest {

    private val converter = WebAuthorizationConfig().jwtAuthenticationConverter()

    @Test
    fun `combines authorities and scopes into granted authorities`() {
        val jwt = TestJwtUtil.jwt(
            authorities = listOf("ROLE_ADMIN", "ROLE_USER"),
            scopes = listOf("profile", "email")
        )

        val auth = converter.convert(jwt)!!
        val granted = auth.authorities.map { it.authority }.toSet()

        assertTrue(granted.contains("ROLE_ADMIN"))
        assertTrue(granted.contains("ROLE_USER"))
        assertTrue(granted.contains("SCOPE_profile"))
        assertTrue(granted.contains("SCOPE_email"))
        assertEquals(jwt.subject, auth.name)
    }

    @Test
    fun `gracefully handles missing authorities and scope claims`() {
        val jwt = TestJwtUtil.jwt(extraClaims = mapOf())
        val auth = converter.convert(jwt)!!
        assertTrue(auth.authorities.isEmpty())
    }
}

