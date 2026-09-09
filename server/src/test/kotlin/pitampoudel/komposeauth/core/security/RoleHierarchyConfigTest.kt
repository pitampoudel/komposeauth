package pitampoudel.komposeauth.core.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import pitampoudel.komposeauth.core.domain.Roles
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoleHierarchyConfigTest {

    private val hierarchy = RoleHierarchyConfig().roleHierarchy()

    private fun reachableFrom(role: String): Set<String?> =
        hierarchy.getReachableGrantedAuthorities(listOf(SimpleGrantedAuthority("ROLE_$role")))
            .mapTo(mutableSetOf()) { it.authority }

    @Test
    fun `a super admin holds everything an admin holds`() {
        assertTrue(
            "ROLE_${Roles.ADMIN}" in reachableFrom(Roles.SUPER_ADMIN),
            "a SUPER_ADMIN must satisfy every hasRole('ADMIN') rule"
        )
    }

    @Test
    fun `an admin is not a super admin`() {
        // SUPER_ADMIN gates the configuration page and every secret on it. An ADMIN inheriting it
        // would be the one direction of this that matters.
        assertFalse(
            "ROLE_${Roles.SUPER_ADMIN}" in reachableFrom(Roles.ADMIN),
            "ADMIN must not inherit SUPER_ADMIN"
        )
    }
}
