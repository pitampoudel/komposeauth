package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.user.service.UserService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class UserServiceAdminEdgeCasesTest {

    @Test
    fun `revokeAdmin throws when trying to remove last admin`() {
        val userRepo = mock<UserRepository>()
        val user = User(
            id = ObjectId.get(),
            firstName = "Admin",
            lastName = "User",
            email = "admin@example.com",
            phoneNumber = null,
            passwordHash = "hash",
            roles = listOf("ADMIN"),
        )

        whenever(userRepo.findById(user.id)).thenReturn(Optional.of(user))
        whenever(userRepo.countByRolesContaining("ADMIN")).thenReturn(1)

        val service = UserService(
            userRepository = userRepo,
            passwordEncoder = mock(),
            phoneNumberVerificationService = mock(),
            appConfigService = mock(),
            emailService = mock(),
            oneTimeTokenService = mock(),
            kycService = mock(),
            storageService = mock(),
            objectMapper = mock(),
            webAuthnRelyingPartyOperations = mock(),
            roleChangeEmailNotifier = mock(),
            emailVerificationService = mock(),
            appleTokenValidator = mock()
        )

        assertThrows<org.apache.coyote.BadRequestException> {
            service.revokeAdmin("Test Admin", user.id.toHexString())
        }
    }

    @Test
    fun `grantAdmin throws UsernameNotFoundException for invalid object id`() {
        val userRepo = mock<UserRepository>()

        val service = UserService(
            userRepository = userRepo,
            passwordEncoder = mock(),
            phoneNumberVerificationService = mock(),
            appConfigService = mock(),
            emailService = mock(),
            oneTimeTokenService = mock(),
            kycService = mock(),
            storageService = mock(),
            objectMapper = mock(),
            webAuthnRelyingPartyOperations = mock(),
            roleChangeEmailNotifier = mock(),
            emailVerificationService = mock(),
            appleTokenValidator = mock()

        )

        assertThrows<UsernameNotFoundException> {
            service.grantAdmin("Test Admin", "not-an-object-id")
        }
    }

    @Test
    fun `findUsersFlexible caps size larger than 200 and coerces negative page`() {
        val userRepo = mock<UserRepository>()

        val emptyPage: Page<User> = PageImpl(emptyList())
        whenever(userRepo.findAll(org.mockito.kotlin.any<Pageable>())).thenReturn(emptyPage)

        val service = UserService(
            userRepository = userRepo,
            passwordEncoder = mock(),
            phoneNumberVerificationService = mock(),
            appConfigService = mock(),
            emailService = mock(),
            oneTimeTokenService = mock(),
            kycService = mock(),
            storageService = mock(),
            objectMapper = mock(),
            webAuthnRelyingPartyOperations = mock(),
            roleChangeEmailNotifier = mock(),
            emailVerificationService = mock(),
            appleTokenValidator = mock()
        )

        val result = service.findUsersFlexible(ids = null, q = null, page = -10, size = 9999)
        assertTrue(result.content.isEmpty())

        // Assert the service passed a sanitized Pageable downstream.
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(userRepo).findAll(pageableCaptor.capture())
        assertEquals(0, pageableCaptor.firstValue.pageNumber)
        assertEquals(200, pageableCaptor.firstValue.pageSize)
    }

    @Test
    fun `findUsersFlexible uses regex-based case-insensitive search with trimmed query`() {
        val userRepo = mock<UserRepository>()
        val emptyPage: Page<User> = PageImpl(emptyList())
        whenever(userRepo.searchUsersCaseInsensitive(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(emptyPage)

        val service = UserService(
            userRepository = userRepo,
            passwordEncoder = mock(),
            phoneNumberVerificationService = mock(),
            appConfigService = mock(),
            emailService = mock(),
            oneTimeTokenService = mock(),
            kycService = mock(),
            storageService = mock(),
            objectMapper = mock(),
            webAuthnRelyingPartyOperations = mock(),
            roleChangeEmailNotifier = mock(),
            emailVerificationService = mock(),
            appleTokenValidator = mock()
        )

        service.findUsersFlexible(ids = null, q = "  MixedCase  ", page = 1, size = 25)

        val regexCaptor = argumentCaptor<String>()
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(userRepo).searchUsersCaseInsensitive(regexCaptor.capture(), pageableCaptor.capture())
        assertEquals(".*MixedCase.*", regexCaptor.firstValue)
        assertEquals(25, pageableCaptor.firstValue.pageSize)
        assertEquals(1, pageableCaptor.firstValue.pageNumber)
    }

    @Test
    fun `findUsersFlexible searches by full name when query has multiple parts`() {
        val userRepo = mock<UserRepository>()
        val emptyPage: Page<User> = PageImpl(emptyList())
        whenever(userRepo.searchUsersByFullNameCaseInsensitive(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(emptyPage)

        val service = UserService(
            userRepository = userRepo,
            passwordEncoder = mock(),
            phoneNumberVerificationService = mock(),
            appConfigService = mock(),
            emailService = mock(),
            oneTimeTokenService = mock(),
            kycService = mock(),
            storageService = mock(),
            objectMapper = mock(),
            webAuthnRelyingPartyOperations = mock(),
            roleChangeEmailNotifier = mock(),
            emailVerificationService = mock(),
            appleTokenValidator = mock()
        )

        service.findUsersFlexible(ids = null, q = "John    Doe", page = 2, size = 15)

        val firstRegexCaptor = argumentCaptor<String>()
        val lastRegexCaptor = argumentCaptor<String>()
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(userRepo).searchUsersByFullNameCaseInsensitive(firstRegexCaptor.capture(), lastRegexCaptor.capture(), pageableCaptor.capture())
        assertEquals(".*John.*", firstRegexCaptor.firstValue)
        assertEquals(".*Doe.*", lastRegexCaptor.firstValue)
        assertEquals(15, pageableCaptor.firstValue.pageSize)
        assertEquals(2, pageableCaptor.firstValue.pageNumber)
    }
}
