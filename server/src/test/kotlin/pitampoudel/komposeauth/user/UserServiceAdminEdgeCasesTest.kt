package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.user.service.UserService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit tests: [UserService] is built from mocks, so no Spring context (and no Mongo container)
 * is needed.
 */
class UserServiceAdminEdgeCasesTest {

    /** The admin performing a role change. Only their roles and name are read. */
    private fun actor(roles: List<String> = listOf(Roles.ADMIN)) = User(
        id = ObjectId.get(),
        firstName = "Test",
        lastName = "Admin",
        email = "actor@example.com",
        phoneNumber = null,
        roles = roles
    )

    private fun appConfigService(roles: List<String> = Roles.BUILT_IN) =
        mock<AppConfigService>().also { whenever(it.availableRoles()).thenReturn(roles) }

    private fun service(
        userRepository: UserRepository,
        appConfigService: AppConfigService = appConfigService()
    ) = UserService(
        userRepository = userRepository,
        passwordEncoder = mock(),
        phoneNumberVerificationService = mock(),
        appConfigService = appConfigService,
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
        appleTokenValidator = mock(),
        oauth2AuthorizationDocumentRepository = mock()
    )

    private fun user(roles: List<String> = emptyList()) = User(
        id = ObjectId.get(),
        firstName = "Target",
        lastName = "User",
        email = "target@example.com",
        phoneNumber = null,
        passwordHash = "hash",
        roles = roles
    )

    @Test
    fun `revokeRole throws when trying to remove last admin`() {
        val userRepo = mock<UserRepository>()
        val target = user(roles = listOf(Roles.ADMIN))

        whenever(userRepo.findById(target.id)).thenReturn(Optional.of(target))
        whenever(userRepo.countByRolesContaining(Roles.ADMIN)).thenReturn(1)

        assertThrows<org.apache.coyote.BadRequestException> {
            service(userRepo).revokeRole(actor(), target.id.toHexString(), Roles.ADMIN)
        }
    }

    @Test
    fun `revokeRole removes the role when other holders remain`() {
        val userRepo = mock<UserRepository>()
        val target = user(roles = listOf(Roles.ADMIN, "SUPPORT"))

        whenever(userRepo.findById(target.id)).thenReturn(Optional.of(target))
        whenever(userRepo.countByRolesContaining(Roles.ADMIN)).thenReturn(2)
        whenever(userRepo.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val updated = service(userRepo, appConfigService(Roles.BUILT_IN + "SUPPORT"))
            .revokeRole(actor(), target.id.toHexString(), Roles.ADMIN)

        assertEquals(listOf("SUPPORT"), updated.roles)
    }

    @Test
    fun `revokeRole allows removing the last holder of an unprotected role`() {
        val userRepo = mock<UserRepository>()
        val target = user(roles = listOf("SUPPORT"))

        whenever(userRepo.findById(target.id)).thenReturn(Optional.of(target))
        whenever(userRepo.countByRolesContaining("SUPPORT")).thenReturn(1)
        whenever(userRepo.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val updated = service(userRepo, appConfigService(Roles.BUILT_IN + "SUPPORT"))
            .revokeRole(actor(), target.id.toHexString(), "SUPPORT")

        assertTrue(updated.roles.isEmpty())
    }

    @Test
    fun `grantRole throws UsernameNotFoundException for invalid object id`() {
        assertThrows<UsernameNotFoundException> {
            service(mock()).grantRole(actor(), "not-an-object-id", Roles.ADMIN)
        }
    }

    @Test
    fun `grantRole rejects a role outside the catalog`() {
        val userRepo = mock<UserRepository>()

        assertThrows<org.apache.coyote.BadRequestException> {
            service(userRepo).grantRole(actor(), ObjectId.get().toHexString(), "SUPPORT")
        }
        verify(userRepo, never()).save(any<User>())
    }

    @Test
    fun `grantRole accepts a role once it is in the catalog`() {
        val userRepo = mock<UserRepository>()
        val target = user()

        whenever(userRepo.findById(target.id)).thenReturn(Optional.of(target))
        whenever(userRepo.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val updated = service(userRepo, appConfigService(Roles.BUILT_IN + "SUPPORT"))
            .grantRole(actor(), target.id.toHexString(), "support")

        assertEquals(listOf("SUPPORT"), updated.roles)
    }

    @Test
    fun `grantRole denies SUPER_ADMIN to an actor who is only an ADMIN`() {
        val userRepo = mock<UserRepository>()

        assertThrows<AccessDeniedException> {
            service(userRepo).grantRole(actor(), ObjectId.get().toHexString(), Roles.SUPER_ADMIN)
        }
        verify(userRepo, never()).save(any<User>())
    }

    @Test
    fun `grantRole allows SUPER_ADMIN when the actor is a SUPER_ADMIN`() {
        val userRepo = mock<UserRepository>()
        val target = user()

        whenever(userRepo.findById(target.id)).thenReturn(Optional.of(target))
        whenever(userRepo.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val updated = service(userRepo).grantRole(
            actor(roles = listOf(Roles.ADMIN, Roles.SUPER_ADMIN)),
            target.id.toHexString(),
            Roles.SUPER_ADMIN
        )

        assertEquals(listOf(Roles.SUPER_ADMIN), updated.roles)
    }

    @Test
    fun `listRoles reports the catalog with holder counts`() {
        val userRepo = mock<UserRepository>()
        whenever(userRepo.countByRolesContaining(any())).thenReturn(0)
        whenever(userRepo.countByRolesContaining(Roles.ADMIN)).thenReturn(3)

        val roles = service(userRepo, appConfigService(Roles.BUILT_IN + "SUPPORT")).listRoles()

        assertEquals(listOf(Roles.ADMIN, Roles.SUPER_ADMIN, "SUPPORT"), roles.map { it.role })
        assertEquals(3, roles.first { it.role == Roles.ADMIN }.userCount)
        assertEquals(listOf(true, true, false), roles.map { it.builtIn })
    }

    @Test
    fun `findUsersFlexible caps size larger than 200 and coerces negative page`() {
        val userRepo = mock<UserRepository>()

        val emptyPage: Page<User> = PageImpl(emptyList())
        whenever(userRepo.findAll(any<Pageable>())).thenReturn(emptyPage)

        val result = service(userRepo).findUsersFlexible(
            ids = null,
            q = null,
            page = -10,
            size = 9999
        )
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
        whenever(userRepo.search(any(), anyOrNull(), any())).thenReturn(emptyPage)

        service(userRepo).findUsersFlexible(ids = null, q = "  MixedCase  ", page = 1, size = 25)

        val tokensCaptor = argumentCaptor<List<String>>()
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(userRepo).search(tokensCaptor.capture(), isNull(), pageableCaptor.capture())
        assertEquals(listOf("MixedCase"), tokensCaptor.firstValue)
        assertEquals(25, pageableCaptor.firstValue.pageSize)
        assertEquals(1, pageableCaptor.firstValue.pageNumber)
    }

    @Test
    fun `findUsersFlexible searches by full name when query has multiple parts`() {
        val userRepo = mock<UserRepository>()
        val emptyPage: Page<User> = PageImpl(emptyList())
        whenever(userRepo.search(any(), anyOrNull(), any())).thenReturn(emptyPage)

        service(userRepo).findUsersFlexible(ids = null, q = "John    Doe", page = 2, size = 15)

        val tokensCaptor = argumentCaptor<List<String>>()
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(userRepo).search(tokensCaptor.capture(), isNull(), pageableCaptor.capture())
        assertEquals(listOf("John", "Doe"), tokensCaptor.firstValue)
        assertEquals(15, pageableCaptor.firstValue.pageSize)
        assertEquals(2, pageableCaptor.firstValue.pageNumber)
    }

    @Test
    fun `findUsersFlexible passes a normalized role filter to the repository`() {
        val userRepo = mock<UserRepository>()
        val emptyPage: Page<User> = PageImpl(emptyList())
        whenever(userRepo.search(any(), anyOrNull(), any())).thenReturn(emptyPage)

        service(userRepo).findUsersFlexible(
            ids = null,
            q = null,
            role = " super-admin ",
            page = 0,
            size = 50
        )

        val tokensCaptor = argumentCaptor<List<String>>()
        verify(userRepo).search(tokensCaptor.capture(), eq(Roles.SUPER_ADMIN), any())
        assertTrue(tokensCaptor.firstValue.isEmpty())
    }
}
