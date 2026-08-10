package pitampoudel.komposeauth.templates

import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.thymeleaf.context.WebContext
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.web.servlet.JakartaServletWebApplication
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Renders the server-rendered pages without booting the app, so a broken template is caught
 * without a Mongo container. Uses [SpringTemplateEngine] so expressions evaluate through SpEL,
 * the way they do in production. Anything genuinely Spring-MVC-only (`@{...}` URLs, `_csrf`) is
 * left out of the model deliberately.
 */
class TemplateRenderingTest {

    private val applicationContext = GenericApplicationContext().apply { refresh() }

    private val engine = SpringTemplateEngine().apply {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
            isCacheable = false
        })
    }

    private val webApplication =
        JakartaServletWebApplication.buildApplication(MockServletContext())

    /** A web exchange, so `@{...}` link expressions resolve the way they do behind Spring MVC. */
    private fun exchange() = webApplication.buildExchange(
        MockHttpServletRequest(),
        MockHttpServletResponse()
    )

    private fun brandingContext(vararg extra: Pair<String, Any>) = WebContext(exchange()).apply {
        setVariable(
            ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
            ThymeleafEvaluationContext(applicationContext, null)
        )
        setVariable("appName", "Bolne Page")
        setVariable("logoUrl", "")
        setVariable("brandColor", "#3458d4")
        extra.forEach { (key, value) -> setVariable(key, value) }
    }

    @Test
    fun `login page renders both views and the branded heading`() {
        val html = engine.process("session-login", brandingContext())

        assertContains(html, "Welcome back")
        assertContains(html, "Sign in to continue to Bolne Page")
        // The forgot-password view used to render a subheading with no title above it.
        assertContains(html, "Reset your password")
        assertContains(html, "id=\"view-forgot\"")
    }

    @Test
    fun `login page shows the failure message it was given`() {
        val html = engine.process(
            "session-login",
            brandingContext("error" to "This account has been deactivated. Contact support to get it reopened.")
        )

        assertContains(html, "This account has been deactivated.")
        assertContains(html, "alert-error")
    }

    @Test
    fun `login page omits Google when it is not configured`() {
        val enabled = engine.process("session-login", brandingContext("googleEnabled" to true))
        assertContains(enabled, "/oauth2/authorization/google")

        val disabled = engine.process("session-login", brandingContext("googleEnabled" to false))
        assertFalse(
            disabled.contains("/oauth2/authorization/google"),
            "Google button should not render when Google sign-in is unconfigured"
        )
    }

    @Test
    fun `users dashboard renders and inlines the viewer's roles`() {
        val html = engine.process(
            "users-dashboard",
            brandingContext("viewerRoles" to listOf("ADMIN", "SUPER_ADMIN"))
        )

        assertContains(html, "People and roles")
        assertContains(html, "/oauth2/clients/dashboard")
        // The page disables SUPER_ADMIN controls for a viewer who lacks it, so the roles have to
        // survive inlining as real JSON.
        assertContains(html, "\"SUPER_ADMIN\"")
        assertTrue(
            html.contains("const VIEWER_ROLES = [\"ADMIN\",\"SUPER_ADMIN\"]"),
            "viewerRoles should inline as a JSON array, was: " +
                    html.lineSequence().first { it.contains("VIEWER_ROLES") }.trim()
        )
    }

    @Test
    fun `oauth2 clients page renders with its cross-links`() {
        val html = engine.process("oauth2-clients", brandingContext())

        assertContains(html, "OAuth2 Clients")
        assertContains(html, "/users/dashboard")
    }

    @Test
    fun `users dashboard falls back when the app has no name or logo`() {
        val context = brandingContext("viewerRoles" to emptyList<String>())
        context.setVariable("appName", "")

        val html = engine.process("users-dashboard", context)

        assertContains(html, "Admin")
        assertContains(html, "const VIEWER_ROLES = []")
    }
}
