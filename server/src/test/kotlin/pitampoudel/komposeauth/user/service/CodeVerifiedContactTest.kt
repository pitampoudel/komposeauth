package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.email.EmailVerificationService
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyCredentialRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The pre-account takeover of [ProviderVerifiedEmailTest], arriving by a code instead: signing in
 * with a code sent to an address or number is the first proof of who holds it, so whatever the
 * registrant set up on it unproven goes.
 */
class CodeVerifiedContactTest {

    private val email = "victim@example.com"
    private val phone = "+9779812345678"

    private val emails = mock<EmailVerificationService>().also { whenever(it.verify(any(), any())).thenReturn(true) }
    private val phones = mock<PhoneNumberVerificationService>().also { whenever(it.verify(any(), any())).thenReturn(true) }
    private val passkeys = mock<PublicKeyCredentialRepository>()
    private val revocation = mock<AccessRevocation>()

    private fun account(emailVerified: Boolean = false, phoneNumberVerified: Boolean = false) = User(
        id = ObjectId.get(),
        firstName = null,
        lastName = null,
        email = email,
        emailVerified = emailVerified,
        phoneNumber = phone,
        phoneNumberVerified = phoneNumberVerified,
        passwordHash = "registrant-hash"
    )

    private fun repositoryHolding(user: User) = mock<UserRepository>().also { repo ->
        whenever(repo.findByUserName(email)).thenReturn(user)
        whenever(repo.findByUserName(phone)).thenReturn(user)
        whenever(repo.save(any<User>())).thenAnswer { it.arguments[0] as User }
    }

    private fun service(userRepository: UserRepository) = UserService(
        userRepository = userRepository,
        passwordEncoder = mock(),
        phoneNumberVerificationService = phones,
        appConfigService = mock<AppConfigService>().also { whenever(it.defaultPhoneRegion()).thenReturn("NP") },
        emailService = mock(),
        oneTimeTokenService = mock(),
        kycService = mock(),
        kycVerificationRepository = mock(),
        publicKeyUserRepository = mock(),
        publicKeyCredentialRepository = passkeys,
        organizationRepository = mock(),
        oneTimeTokenRepository = mock(),
        storageService = mock(),
        passkeys = mock(),
        roleChangeEmailNotifier = mock(),
        emailVerificationService = emails,
        appleTokenValidator = mock(),
        accessRevocation = revocation,
        consentRepository = mock(),
        contactChangeNotifier = mock()
    )

    @Test
    fun `a code proving an address for the first time clears the password and passkeys it was registered with`() {
        val registered = account()
        val repo = repositoryHolding(registered)

        val user = service(repo).resolveUserFromCredential(Credential.OTP(username = email, otp = "123456"))

        assertTrue(user.emailVerified)
        assertNull(user.passwordHash)
        val saved = argumentCaptor<User>()
        verify(repo).save(saved.capture())
        assertNull(saved.firstValue.passwordHash)
        verify(passkeys).deleteAllByUserId(registered.id)
        verify(revocation).revokeAll(registered.id)
    }

    @Test
    fun `a code proving a phone number for the first time does the same`() {
        val registered = account(emailVerified = true)
        val repo = repositoryHolding(registered)

        val user = service(repo).resolveUserFromCredential(Credential.OTP(username = phone, otp = "123456"))

        assertTrue(user.phoneNumberVerified)
        assertNull(user.passwordHash)
        verify(passkeys).deleteAllByUserId(registered.id)
        verify(revocation).revokeAll(registered.id)
    }

    @Test
    fun `a code for an address already proven leaves the password and passkeys alone`() {
        val owner = account(emailVerified = true)
        val repo = repositoryHolding(owner)

        val user = service(repo).resolveUserFromCredential(Credential.OTP(username = email, otp = "123456"))

        assertEquals("registrant-hash", user.passwordHash)
        verify(passkeys, never()).deleteAllByUserId(any())
        verify(revocation, never()).revokeAll(any())
    }
}
