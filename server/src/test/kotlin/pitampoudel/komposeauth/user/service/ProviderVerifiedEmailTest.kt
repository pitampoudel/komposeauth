package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The pre-account takeover: somebody registers the victim's address with a password of their own,
 * the victim later signs in through Google or Apple and takes the account as theirs, and the
 * registrant's password goes on working beside them. The provider is the first proof of who owns
 * the mailbox, so it is where the password goes.
 */
class ProviderVerifiedEmailTest {

    private val email = "victim@example.com"

    private fun account(emailVerified: Boolean, passwordHash: String? = "registrant-hash") = User(
        id = ObjectId.get(),
        firstName = null,
        lastName = null,
        email = email,
        emailVerified = emailVerified,
        phoneNumber = null,
        passwordHash = passwordHash
    )

    private fun repositoryHolding(user: User) = mock<UserRepository>().also { repo ->
        whenever(repo.findByUserName(email)).thenReturn(user)
        whenever(repo.save(any<User>())).thenAnswer { it.arguments[0] as User }
    }

    private fun service(
        userRepository: UserRepository,
        accessRevocation: AccessRevocation,
        appleTokenValidator: AppleTokenValidator = mock()
    ) = UserService(
        userRepository = userRepository,
        passwordEncoder = mock(),
        phoneNumberVerificationService = mock(),
        appConfigService = mock<AppConfigService>().also {
            whenever(it.getConfig()).thenReturn(AppConfig(appleAuthClientId = "apple-client"))
        },
        emailService = mock(),
        oneTimeTokenService = mock(),
        kycService = mock(),
        kycVerificationRepository = mock(),
        publicKeyUserRepository = mock(),
        publicKeyCredentialRepository = mock(),
        organizationRepository = mock(),
        oneTimeTokenRepository = mock(),
        storageService = mock(),
        objectMapper = mock(),
        webAuthnRelyingPartyOperations = mock(),
        roleChangeEmailNotifier = mock(),
        emailVerificationService = mock(),
        appleTokenValidator = appleTokenValidator,
        accessRevocation = accessRevocation,
        consentRepository = mock()
    )

    @Test
    fun `Google proving an address for the first time clears the password it was registered with`() {
        val registered = account(emailVerified = false)
        val repo = repositoryHolding(registered)
        val revocation = mock<AccessRevocation>()

        val user = service(repo, revocation).findOrCreateVerifiedGoogleUser(
            profile = CreateUserRequest(email = email),
            emailVerified = true
        )

        assertTrue(user.emailVerified)
        assertNull(user.passwordHash)
        val saved = argumentCaptor<User>()
        verify(repo).save(saved.capture())
        assertNull(saved.firstValue.passwordHash)
        // Whoever signed in with that password is signed out too, not just kept from doing it again.
        verify(revocation).revokeAll(registered.id)
    }

    @Test
    fun `Apple proving an address for the first time clears the password too`() {
        val registered = account(emailVerified = false)
        val repo = repositoryHolding(registered)
        val revocation = mock<AccessRevocation>()
        val apple = mock<AppleTokenValidator>()
        whenever(apple.validate(any(), eq("apple-client"))).thenReturn(
            Jwt.withTokenValue("apple-id-token")
                .header("alg", "RS256")
                .claim("email", email)
                .claim("email_verified", "true")
                .build()
        )

        val user = service(repo, revocation, apple)
            .resolveUserFromCredential(Credential.AppleId("apple-id-token")) { null }

        assertTrue(user.emailVerified)
        assertNull(user.passwordHash)
        verify(revocation).revokeAll(registered.id)
    }

    @Test
    fun `a provider leaves the password of an address already proven alone`() {
        val owner = account(emailVerified = true)
        val repo = repositoryHolding(owner)
        val revocation = mock<AccessRevocation>()

        val user = service(repo, revocation).findOrCreateVerifiedGoogleUser(
            profile = CreateUserRequest(email = email),
            emailVerified = true
        )

        assertEquals("registrant-hash", user.passwordHash)
        verify(repo, never()).save(any<User>())
        verify(revocation, never()).revokeAll(any())
        verify(revocation, never()).endSessions(any(), anyOrNull())
    }
}
