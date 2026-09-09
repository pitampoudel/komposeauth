package pitampoudel.komposeauth.core.security

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import pitampoudel.komposeauth.core.domain.Roles
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * That the hierarchy actually reaches `@PreAuthorize`, which is where nearly every rule in this
 * application lives.
 *
 * Declaring a [org.springframework.security.access.hierarchicalroles.RoleHierarchy] bean is not on
 * its own enough to be sure of that — method security builds its own expression handler, and
 * whether it picks the bean up is a property of the framework version rather than of this code. So
 * this boots the real configuration and asks a real guarded method, rather than asserting against
 * the hierarchy object alone.
 */
class RoleHierarchyMethodSecurityTest {

    open class AdminOnlyService {
        @PreAuthorize("hasRole('${Roles.ADMIN}')")
        open fun adminOnly(): String = "reached"
    }

    @Configuration
    @EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
    open class Harness {
        @Bean
        open fun adminOnlyService(): AdminOnlyService = AdminOnlyService()
    }

    private fun guarded(): AdminOnlyService =
        AnnotationConfigApplicationContext(RoleHierarchyConfig::class.java, Harness::class.java)
            .getBean(AdminOnlyService::class.java)

    private fun signInAs(vararg roles: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            "someone",
            null,
            roles.map { SimpleGrantedAuthority("ROLE_$it") }
        )
    }

    @AfterTest
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `a super admin satisfies a hasRole ADMIN rule`() {
        val service = guarded()
        signInAs(Roles.SUPER_ADMIN)

        assertEquals("reached", service.adminOnly())
    }

    @Test
    fun `an admin still satisfies it directly`() {
        val service = guarded()
        signInAs(Roles.ADMIN)

        assertEquals("reached", service.adminOnly())
    }

    @Test
    fun `an ordinary user does not`() {
        val service = guarded()
        signInAs("USER")

        assertFailsWith<AccessDeniedException> { service.adminOnly() }
    }
}
