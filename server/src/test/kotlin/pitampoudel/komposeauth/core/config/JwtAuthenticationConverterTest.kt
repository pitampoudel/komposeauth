package pitampoudel.komposeauth.core.config

import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.TestJwtUtil
import pitampoudel.komposeauth.core.security.WebAuthorizationConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Import(TestConfig::class)
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
}

