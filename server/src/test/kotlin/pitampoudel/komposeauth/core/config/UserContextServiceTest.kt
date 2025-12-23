package pitampoudel.komposeauth.core.config

import org.bson.types.ObjectId
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class UserContextServiceTest {

    private val userService: UserService = mock()
    private val sut = UserContextService(userService)

    private fun user(username: String) = User(
        id = ObjectId.get(),
        firstName = "Test",
        lastName = "User",
        email = username,
        emailVerified = false,
        phoneNumber = null,
        phoneNumberVerified = false,
        picture = null,
        socialLinks = emptyList(),
        passwordHash = null,
        roles = listOf("USER"),
        deactivated = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `jwt authentication resolves user by subject when not client credentials`() {
        val jwt = Jwt(
            "token",
            Instant.now().minusSeconds(5),
            Instant.now().plusSeconds(60),
            mapOf("alg" to "none"),
            mapOf("sub" to "alice")
        )
        val expected = user("alice")
        whenever(userService.findByUserName("alice")).thenReturn(expected)

        val auth = JwtAuthenticationToken(jwt)
        val actual = sut.getUserFromAuthentication(auth)
        assertEquals(expected, actual)
    }

    @Test
    fun `jwt authentication with client_id claim is rejected`() {
        val jwt = Jwt(
            "token",
            Instant.now().minusSeconds(5),
            Instant.now().plusSeconds(60),
            mapOf("alg" to "none"),
            mapOf("sub" to "alice", "client_id" to "some-client")
        )
        val auth = JwtAuthenticationToken(jwt)
        assertThrows<IllegalStateException> { sut.getUserFromAuthentication(auth) }
    }

    @Test
    fun `jwt authentication without subject is rejected`() {
        // Spring Security's Jwt enforces a non-null/non-empty subject at construction time.
        // That means the failure happens before our service is invoked.
        assertThrows<IllegalArgumentException> {
            Jwt(
                "token",
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(60),
                mapOf("alg" to "none"),
                mapOf<String, Any>()
            )
        }
    }

    @Test
    fun `jwt authentication throws when user not found`() {
        val jwt = Jwt(
            "token",
            Instant.now().minusSeconds(5),
            Instant.now().plusSeconds(60),
            mapOf("alg" to "none"),
            mapOf("sub" to "missing")
        )
        whenever(userService.findByUserName("missing")).thenReturn(null)

        val auth = JwtAuthenticationToken(jwt)
        assertThrows<IllegalStateException> { sut.getUserFromAuthentication(auth) }
    }

    @Test
    fun `username password authentication resolves user by name`() {
        val expected = user("bob@example.com")
        whenever(userService.findByUserName("bob@example.com")).thenReturn(expected)

        val auth = UsernamePasswordAuthenticationToken("bob@example.com", "pw")
        val actual = sut.getUserFromAuthentication(auth)
        assertEquals(expected, actual)
    }

    @Test
    fun `unsupported authentication type throws`() {
        val auth = object : org.springframework.security.core.Authentication {
            override fun getName() = "x"
            override fun getAuthorities() = emptyList<org.springframework.security.core.GrantedAuthority>()
            override fun getCredentials() = null
            override fun getDetails() = null
            override fun getPrincipal() = "x"
            override fun isAuthenticated() = true
            override fun setAuthenticated(isAuthenticated: Boolean) {}
        }

        assertThrows<IllegalStateException> { sut.getUserFromAuthentication(auth) }
    }
}
