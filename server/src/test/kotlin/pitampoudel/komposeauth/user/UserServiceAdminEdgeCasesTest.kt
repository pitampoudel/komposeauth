package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.user.service.UserService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
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
        )

        assertThrows<org.apache.coyote.BadRequestException> {
            service.revokeAdmin(user.id.toHexString())
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
        )

        assertThrows<UsernameNotFoundException> {
            service.grantAdmin("not-an-object-id")
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
        )

        val result = service.findUsersFlexible(ids = null, q = null, page = -10, size = 9999)
        assertTrue(result.content.isEmpty())

        // Assert the service passed a sanitized Pageable downstream.
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(userRepo).findAll(pageableCaptor.capture())
        assertEquals(0, pageableCaptor.firstValue.pageNumber)
        assertEquals(200, pageableCaptor.firstValue.pageSize)
    }
}
