package pitampoudel.komposeauth.user

import kotlinx.serialization.json.Json
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.app_config.controller.AppConfigController
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class FirstAdminIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    /** Other tests share the database; whoever holds the role now gets it back afterwards. */
    private var holders: List<User> = emptyList()

    @BeforeEach
    fun noSuperAdmin() {
        holders = userRepository.findAll().filter { Roles.SUPER_ADMIN in it.roles }
        holders.forEach { userRepository.save(it.copy(roles = it.roles - Roles.SUPER_ADMIN)) }
    }

    @AfterEach
    fun restore() {
        userRepository.findAll().filter { Roles.SUPER_ADMIN in it.roles }
            .forEach { userRepository.save(it.copy(roles = it.roles - Roles.SUPER_ADMIN)) }
        holders.forEach { holder ->
            userRepository.findById(holder.id).ifPresent { userRepository.save(it.copy(roles = holder.roles)) }
        }
    }

    private fun createFirstAdmin(email: String, password: String? = null) = mockMvc.post("/admin/config") {
        param("key", TestConfig.testKey)
        param("firstAdmin", "1")
        param("adminEmail", email)
        param("adminName", "Ada Lovelace")
        password?.let { param("adminPassword", it) }
    }

    @Test
    fun `a fresh server offers to create the first admin, once`() {
        mockMvc.get("/admin/config") {
            header(AppConfigController.MASTER_KEY_HEADER, TestConfig.testKey)
        }.andExpect { content { string(containsString("Create the first admin")) } }

        createFirstAdmin("First.Admin@Example.com", "correct horse").andExpect {
            status { isOk() }
            content { string(containsString("first.admin@example.com is now a super admin")) }
            content { string(not(containsString("Create the first admin"))) }
        }

        val admin = userRepository.findByEmail("first.admin@example.com")!!
        assertEquals(listOf(Roles.ADMIN, Roles.SUPER_ADMIN), admin.roles)
        assertEquals("Ada", admin.firstName)
        assertEquals("Lovelace", admin.lastName)
        assertTrue(admin.emailVerified)
        assertTrue(passwordEncoder.matches("correct horse", admin.passwordHash))

        createFirstAdmin("second.admin@example.com", "correct horse").andExpect {
            status { isBadRequest() }
            content { string(containsString("already has a super admin")) }
        }
        assertEquals(null, userRepository.findByEmail("second.admin@example.com"))
    }

    @Test
    fun `an existing account is promoted and keeps its password`() {
        TestAuthHelpers.createUser(mockMvc, json, "existing.admin@example.com", password = "Password1")

        createFirstAdmin("existing.admin@example.com", "ignored password").andExpect {
            status { isOk() }
            content { string(containsString("already had an account")) }
        }

        val admin = userRepository.findByEmail("existing.admin@example.com")!!
        assertTrue(Roles.SUPER_ADMIN in admin.roles)
        assertTrue(passwordEncoder.matches("Password1", admin.passwordHash))
    }

    @Test
    fun `a new account needs a real password`() {
        createFirstAdmin("short.password@example.com", "short").andExpect {
            status { isBadRequest() }
            content { string(containsString("at least 8 characters")) }
            content { string(containsString("short.password@example.com")) }
        }
        assertEquals(null, userRepository.findByEmail("short.password@example.com"))
    }

    @Test
    fun `nobody gets in without the key`() {
        mockMvc.post("/admin/config") {
            param("firstAdmin", "1")
            param("adminEmail", "sneaky2@example.com")
            param("adminPassword", "correct horse")
        }.andExpect {
            content { string(containsString("Master key")) }
        }
        assertEquals(null, userRepository.findByEmail("sneaky2@example.com"))
    }
}
