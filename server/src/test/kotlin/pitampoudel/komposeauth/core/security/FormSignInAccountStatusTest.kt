package pitampoudel.komposeauth.core.security

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertFailsWith

/** A closed account is reported as closed only to whoever proves they know its password. */
class FormSignInAccountStatusTest {

    private val encoder = BCryptPasswordEncoder(4)
    private val closed = User(
        id = ObjectId.get(),
        firstName = null,
        lastName = null,
        email = "closed@example.com",
        phoneNumber = null,
        passwordHash = encoder.encode("Password1"),
        deactivated = true
    )

    private val provider = WebSecurityConfig().daoAuthenticationProvider(
        userDetailsService = UserDetailsServiceImpl(
            userRepository = mock<UserRepository> { on { findByLoginName(eq("closed@example.com"), any()) } doReturn closed },
            appConfigService = mock<AppConfigService> { on { defaultPhoneRegion() } doReturn "NP" }
        ),
        passwordEncoder = encoder
    )

    private fun signIn(password: String) =
        provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("closed@example.com", password))

    @Test
    fun `a wrong password is only a wrong password, whatever the account's state`() {
        assertFailsWith<BadCredentialsException> { signIn("wrong-password") }
    }

    @Test
    fun `the right password learns the account is locked`() {
        assertFailsWith<LockedException> { signIn("Password1") }
    }
}
