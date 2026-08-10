package pitampoudel.komposeauth.user.service

import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class UserServiceCoreTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    /** The admin performing a role change. Not persisted — only their roles and name are read. */
    private fun actor(roles: List<String> = listOf(Roles.ADMIN)) = User(
        id = ObjectId.get(),
        firstName = "Test",
        lastName = "Admin",
        email = "actor-${ObjectId.get().toHexString()}@example.com",
        phoneNumber = null,
        roles = roles
    )

    @Test
    fun `findUser returns user when exists`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Find",
                lastName = "Test",
                email = "find-user-test@example.com",
                phoneNumber = null,
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList()
            )
        )

        val found = userService.findUser(user.id.toHexString())

        assertNotNull(found)
        assertEquals(user.id, found?.id)
        assertEquals("Find", found?.firstName)
    }

    @Test
    fun `findUser returns null for non-existent user`() {
        val found = userService.findUser(ObjectId.get().toHexString())
        assertNull(found)
    }

    @Test
    fun `findByUserName returns user by email`() {
        val email = "find-by-username@example.com"
        userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Username",
                lastName = "Test",
                email = email,
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null

            )
        )

        val found = userService.findByUserName(email)

        assertNotNull(found)
        assertEquals(email, found?.email)
    }

    @Test
    fun `findByUserName returns null for non-existent email`() {
        val found = userService.findByUserName("nonexistent@example.com")
        assertNull(found)
    }

    @Test
    fun `grantRole adds ADMIN role to user`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Grant",
                lastName = "Test",
                email = "grant-admin-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null

            )
        )

        val updated = userService.grantRole(actor(), user.id.toHexString(), Roles.ADMIN)

        assertTrue(updated.roles.contains(Roles.ADMIN))
    }

    @Test
    fun `grantRole normalizes the role name`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Normalize",
                lastName = "Test",
                email = "normalize-role-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null
            )
        )

        val updated = userService.grantRole(actor(), user.id.toHexString(), " admin ")

        assertEquals(listOf(Roles.ADMIN), updated.roles)
    }

    @Test
    fun `grantRole rejects a role outside the catalog`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Unknown",
                lastName = "Role",
                email = "unknown-role-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null
            )
        )

        assertThrows(BadRequestException::class.java) {
            userService.grantRole(actor(), user.id.toHexString(), "NOT_IN_CATALOG")
        }
    }

    @Test
    fun `grantRole is idempotent`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Idempotent",
                lastName = "Test",
                email = "idempotent-admin@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = listOf(Roles.ADMIN),
                phoneNumber = null

            )
        )

        val updated = userService.grantRole(actor(), user.id.toHexString(), Roles.ADMIN)

        assertTrue(updated.roles.contains(Roles.ADMIN))
        assertEquals(1, updated.roles.count { it == Roles.ADMIN })
    }

    @Test
    fun `grantRole rejects SUPER_ADMIN when the actor is only an ADMIN`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Escalation",
                lastName = "Test",
                email = "escalation-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null
            )
        )

        assertThrows(AccessDeniedException::class.java) {
            userService.grantRole(actor(), user.id.toHexString(), Roles.SUPER_ADMIN)
        }
    }

    @Test
    fun `grantRole allows SUPER_ADMIN when the actor is a SUPER_ADMIN`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Super",
                lastName = "Target",
                email = "super-target-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null
            )
        )

        val updated = userService.grantRole(
            actor(roles = listOf(Roles.ADMIN, Roles.SUPER_ADMIN)),
            user.id.toHexString(),
            Roles.SUPER_ADMIN
        )

        assertTrue(updated.roles.contains(Roles.SUPER_ADMIN))
    }

    @Test
    fun `emailVerified marks email as verified`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Email",
                lastName = "Test",
                email = "email-verify-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                emailVerified = false,
                phoneNumber = null
            )
        )

        userService.markEmailVerified(user, user.email!!)

        val updated = userRepository.findById(user.id).orElseThrow()
        assertTrue(updated.emailVerified)
    }

    @Test
    fun `findUsersFlexible by role returns only users holding that role`() {
        // Create regular user
        userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Regular",
                lastName = "User",
                email = "regular-list-admins@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null

            )
        )

        // Create admin user
        val admin = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Admin",
                lastName = "User",
                email = "admin-list-admins@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = listOf(Roles.ADMIN),
                phoneNumber = null
            )
        )

        val result = userService.findUsersFlexible(
            ids = null,
            q = null,
            role = Roles.ADMIN,
            page = 0,
            size = 50
        )

        assertTrue(result.content.all { it.roles.contains(Roles.ADMIN) })
        assertTrue(result.content.any { it.id == admin.id })
    }

    @Test
    fun `findUsersFlexible by role supports pagination`() {
        // Create multiple admin users
        repeat(5) { index ->
            userRepository.save(
                User(
                    id = ObjectId.get(),
                    firstName = "Admin$index",
                    lastName = "User",
                    email = "admin-page-$index@example.com",
                    passwordHash = passwordEncoder.encode("Password1"),
                    roles = listOf(Roles.ADMIN),
                    phoneNumber = null
                )
            )
        }

        val page1 = userService.findUsersFlexible(
            ids = null, q = null, role = Roles.ADMIN, page = 0, size = 2
        )
        assertEquals(2, page1.size)
        assertTrue(page1.hasNext())

        val page2 = userService.findUsersFlexible(
            ids = null, q = null, role = Roles.ADMIN, page = 1, size = 2
        )
        assertEquals(2, page2.size)
    }

    @Test
    fun `listRoles reports built-in roles with their user counts`() {
        userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Counted",
                lastName = "Admin",
                email = "counted-admin@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = listOf(Roles.ADMIN),
                phoneNumber = null
            )
        )

        val roles = userService.listRoles()

        assertEquals(Roles.BUILT_IN, roles.map { it.role })
        assertTrue(roles.all { it.builtIn })
        assertTrue(roles.first { it.role == Roles.ADMIN }.userCount >= 1)
    }

    @Test
    fun `deactivate marks user as not active`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Deactivate",
                lastName = "Test",
                email = "deactivate-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null
            )
        )

        userService.deactivateUser(user.id)

        val updated = userRepository.findById(user.id).orElseThrow()
        assertTrue(updated.deactivated)
    }

    @Test
    fun `delete removes user from repository`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Delete",
                lastName = "Test",
                email = "delete-service-test@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = emptyList(),
                phoneNumber = null
            )
        )

        userService.deleteUser(user.id)

        assertFalse(userRepository.existsById(user.id))
    }
}
