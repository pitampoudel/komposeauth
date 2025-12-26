package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
class UserServiceCoreTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

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
    fun `grantAdmin adds ADMIN role to user`() {
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

        val updated = userService.grantAdmin("Test Admin", user.id.toHexString())

        assertTrue(updated.roles.contains("ADMIN"))
    }

    @Test
    fun `grantAdmin is idempotent`() {
        val user = userRepository.save(
            User(
                id = ObjectId.get(),
                firstName = "Idempotent",
                lastName = "Test",
                email = "idempotent-admin@example.com",
                passwordHash = passwordEncoder.encode("Password1"),
                roles = listOf("ADMIN"),
                phoneNumber = null

            )
        )

        val updated = userService.grantAdmin("Test Admin", user.id.toHexString())

        assertTrue(updated.roles.contains("ADMIN"))
        assertEquals(1, updated.roles.count { it == "ADMIN" })
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

        userService.emailVerified(user.id)

        val updated = userRepository.findById(user.id).orElseThrow()
        assertTrue(updated.emailVerified)
    }

    @Test
    fun `listAdmins returns only admin users`() {
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
                roles = listOf("ADMIN"),
                phoneNumber = null
            )
        )

        val result = userService.listAdmins(0, 50)

        assertTrue(result.content.all { it.roles.contains("ADMIN") })
        assertTrue(result.content.any { it.id == admin.id })
    }

    @Test
    fun `listAdmins supports pagination`() {
        // Create multiple admin users
        repeat(5) { index ->
            userRepository.save(
                User(
                    id = ObjectId.get(),
                    firstName = "Admin$index",
                    lastName = "User",
                    email = "admin-page-$index@example.com",
                    passwordHash = passwordEncoder.encode("Password1"),
                    roles = listOf("ADMIN"),
                    phoneNumber = null
                )
            )
        }

        val page1 = userService.listAdmins(0, 2)
        assertEquals(2, page1.size)
        assertTrue(page1.hasNext())

        val page2 = userService.listAdmins(1, 2)
        assertEquals(2, page2.size)
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
}
