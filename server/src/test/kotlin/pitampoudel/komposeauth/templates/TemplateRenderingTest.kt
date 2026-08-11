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
import pitampoudel.komposeauth.core.controller.ConsoleCheck
import pitampoudel.komposeauth.user.data.RoleResponse
import kotlin.test.assertContains
import kotlin.test.assertEquals
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

    /** Branding plus everything `admin/layout.html` reads, as [AdminShell] supplies it. */
    private fun consoleContext(vararg extra: Pair<String, Any>) = brandingContext(
        "userCount" to 142L,
        "clientCount" to 3L,
        "viewerName" to "Pitam Poudel",
        "viewerSub" to "ADMIN",
        "viewerRoles" to listOf("ADMIN"),
        *extra
    )

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
    fun `people page renders and inlines the viewer's roles`() {
        val html = engine.process(
            "admin/users",
            consoleContext("viewerRoles" to listOf("ADMIN", "SUPER_ADMIN"))
        )

        assertContains(html, "People and roles")
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
    fun `applications page renders`() {
        val html = engine.process("admin/clients", consoleContext())

        assertContains(html, "Apps that can ask for tokens")
        assertContains(html, "Register an app")
    }

    @Test
    fun `configuration page renders its groups inside the console`() {
        val html = engine.process(
            "admin/config",
            consoleContext("saved" to true, "fieldGroups" to emptyList<Any>())
        )

        assertContains(html, "Save configuration")
        assertContains(html, "Configuration saved.")
    }

    @Test
    fun `overview states what is on and what is off`() {
        val html = engine.process(
            "admin/overview",
            consoleContext(
                "postureLine" to "142 people can sign in, and 3 apps can ask for tokens on their behalf.",
                "signInMethods" to listOf(ConsoleCheck("Passkeys", true, "Registered against bolnepage.com.")),
                "deliveryChannels" to listOf(ConsoleCheck("Email", false, "SMTP is not set.")),
                "roles" to listOf(RoleResponse("ADMIN", 2, true)),
                "hasCustomRoles" to false
            )
        )

        assertContains(html, "142 people can sign in")
        assertContains(html, "Registered against bolnepage.com.")
        // A check that is off must say so rather than simply be absent.
        assertContains(html, "SMTP is not set.")
        assertContains(html, ">Off<")
    }

    /**
     * Every console page shares one layout, so the navigation is only ever as correct as the
     * `active` argument each page passes into it.
     */
    @Test
    fun `the console marks exactly one navigation entry as current`() {
        val pages = mapOf(
            "admin/users" to "/admin/users",
            "admin/clients" to "/admin/clients",
            "admin/config" to "/admin/config"
        )

        pages.forEach { (template, href) ->
            val html = engine.process(
                template,
                consoleContext("fieldGroups" to emptyList<Any>())
            )
            // Scoped to the rendered <nav>: the layout's stylesheet also mentions the attribute.
            val nav = html.substringAfter("<nav class=\"rail-nav\"").substringBefore("</nav>")
            assertEquals(
                1,
                Regex("aria-current=\"page\"").findAll(nav).count(),
                "$template should light up exactly one navigation entry"
            )
            assertTrue(
                Regex("""href="$href"\s+aria-current="page"""").containsMatchIn(nav),
                "$template should mark $href as the current page"
            )
        }
    }

    @Test
    fun `the console falls back when the app has no name or logo`() {
        val context = consoleContext("viewerRoles" to emptyList<String>())
        context.setVariable("appName", "")

        val html = engine.process("admin/users", context)

        assertContains(html, "Identity")
        assertContains(html, "const VIEWER_ROLES = []")
    }
}
