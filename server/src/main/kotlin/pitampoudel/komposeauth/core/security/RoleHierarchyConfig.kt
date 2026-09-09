package pitampoudel.komposeauth.core.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import pitampoudel.komposeauth.core.domain.Roles

/**
 * Says that a SUPER_ADMIN is also an ADMIN.
 *
 * Without this the two roles were unrelated sets, and every rule written as `hasRole('ADMIN')`
 * silently excluded the more privileged role. The admin console showed the effect plainly: its
 * pages admit `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`, but the APIs they read — `GET /users`,
 * `GET /users/{id}`, the deactivate and find-or-create calls, the API docs — ask only for ADMIN. A
 * super admin could therefore open the user list and watch it fail to load, while a plain admin
 * saw it fine.
 *
 * Stating the hierarchy once fixes every such rule at the source, rather than leaving each new
 * `hasRole('ADMIN')` to remember the other role. It only ever widens SUPER_ADMIN, never ADMIN.
 *
 * What it deliberately does not touch: the checks that read `User.roles` directly rather than
 * granted authorities — the configuration page's own gate, and the rule that only a SUPER_ADMIN may
 * grant or revoke SUPER_ADMIN. Those are asymmetric on purpose, and an ADMIN still cannot reach
 * either.
 */
@Configuration
class RoleHierarchyConfig {

    @Bean
    fun roleHierarchy(): RoleHierarchy = RoleHierarchyImpl.withDefaultRolePrefix()
        .role(Roles.SUPER_ADMIN).implies(Roles.ADMIN)
        .build()

    /**
     * `@PreAuthorize` builds its own expression handler, and the one it defaults to has no
     * hierarchy. Declaring it here is what carries [roleHierarchy] into method security, where
     * nearly every rule in this application lives.
     */
    @Bean
    fun methodSecurityExpressionHandler(
        roleHierarchy: RoleHierarchy
    ): MethodSecurityExpressionHandler = DefaultMethodSecurityExpressionHandler().apply {
        setRoleHierarchy(roleHierarchy)
    }
}
