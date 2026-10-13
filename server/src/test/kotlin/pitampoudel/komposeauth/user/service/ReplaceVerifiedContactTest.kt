package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.core.domain.ErrorCode
import pitampoudel.komposeauth.core.service.email.EmailVerificationService
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import java.time.Duration
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * A verified email or phone number is where a password reset and a code sign-in go, so replacing
 * one takes the account. Somebody holding only a stolen session must not be able to.
 */
class ReplaceVerifiedContactTest {

    private val passwordEncoder = BCryptPasswordEncoder(4)
    private val emails = mock<EmailVerificationService>().also { whenever(it.verify(any(), any())).thenReturn(true) }
    private val phones = mock<PhoneNumberVerificationService>().also { whenever(it.verify(any(), any())).thenReturn(true) }
    private val notifier = mock<ContactChangeNotifier>()
    private val repo = mock<UserRepository>().also { repo ->
        whenever(repo.save(any<User>())).thenAnswer { it.arguments[0] as User }
    }

    private fun account(password: String? = "Password1") = User(
        id = ObjectId.get(),
        firstName = null,
        lastName = null,
        email = "old@example.com",
        emailVerified = true,
        phoneNumber = "+9779812345678",
        phoneNumberVerified = true,
        passwordHash = password?.let(passwordEncoder::encode)
    ).also { whenever(repo.findById(it.id)).thenReturn(Optional.of(it)) }

    private val service = UserService(
        userRepository = repo,
        passwordEncoder = passwordEncoder,
        phoneNumberVerificationService = phones,
        appConfigService = mock(),
        emailService = mock(),
        oneTimeTokenService = mock(),
        kycService = mock(),
        kycVerificationRepository = mock(),
        publicKeyUserRepository = mock(),
        publicKeyCredentialRepository = mock(),
        organizationRepository = mock(),
        oneTimeTokenRepository = mock(),
        storageService = mock(),
        passkeys = mock(),
        roleChangeEmailNotifier = mock(),
        emailVerificationService = emails,
        appleTokenValidator = mock(),
        accessRevocation = mock(),
        consentRepository = mock(),
        contactChangeNotifier = notifier
    )

    private fun refused(code: ErrorCode, block: () -> Unit) {
        val e = assertFailsWith<ResponseStatusException> { block() }
        assertEquals(HttpStatus.FORBIDDEN, e.statusCode)
        assertEquals(code.name, e.body.properties?.get(ErrorCode.PROPERTY))
    }

    @Test
    fun `an account with a password replaces its verified email only with that password`() {
        val user = account()

        refused(ErrorCode.CURRENT_PASSWORD_REQUIRED) { service.verifyEmail(user.id, "new@example.com", "123456") }
        refused(ErrorCode.CURRENT_PASSWORD_REQUIRED) {
            service.verifyEmail(user.id, "new@example.com", "123456", currentPassword = "wrong", signedInAt = Instant.now())
        }
        // Refused before the code is spent, so it can be sent again with the password.
        verify(emails, never()).verify(any(), any())
        verify(repo, never()).save(any<User>())

        val replaced = service.verifyEmail(user.id, "new@example.com", "123456", currentPassword = "Password1")

        assertEquals("new@example.com", replaced.email)
        verify(notifier).emailReplaced(any(), eq("old@example.com"))
    }

    @Test
    fun `an account without a password replaces its verified phone number only soon after signing in`() {
        val user = account(password = null)

        refused(ErrorCode.RECENT_SIGN_IN_REQUIRED) { service.verifyPhoneNumber(user.id, "+9779800000000", "123456") }
        refused(ErrorCode.RECENT_SIGN_IN_REQUIRED) {
            service.verifyPhoneNumber(
                user.id, "+9779800000000", "123456",
                signedInAt = Instant.now() - RECENT_SIGN_IN - Duration.ofMinutes(1)
            )
        }
        verify(phones, never()).verify(any(), any())

        val replaced = service.verifyPhoneNumber(
            user.id, "+9779800000000", "123456", signedInAt = Instant.now() - Duration.ofMinutes(1)
        )

        assertEquals("+9779800000000", replaced.phoneNumber)
        verify(notifier).phoneNumberReplaced(any(), eq("+9779812345678"))
    }

    @Test
    fun `setting a value for the first time or verifying the same one again needs no proof`() {
        val user = account().copy(email = null, emailVerified = false)
            .also { whenever(repo.findById(it.id)).thenReturn(Optional.of(it)) }

        service.verifyEmail(user.id, "first@example.com", "123456")
        service.verifyPhoneNumber(user.id, "+9779812345678", "123456")

        verify(notifier, never()).emailReplaced(any(), anyOrNull())
        verify(notifier, never()).phoneNumberReplaced(any(), anyOrNull())
    }

    @Test
    fun `an unverified address is replaced as freely as it was set`() {
        val user = account().copy(emailVerified = false)
            .also { whenever(repo.findById(it.id)).thenReturn(Optional.of(it)) }

        assertEquals("new@example.com", service.verifyEmail(user.id, "New@Example.com", "123456").email)
        verify(notifier, never()).emailReplaced(any(), anyOrNull())
    }
}
