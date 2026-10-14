package pitampoudel.komposeauth.app_config.controller

import io.swagger.v3.oas.annotations.Operation
import kotlinx.serialization.Serializable
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.StartupRunner
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.app_config.service.MasterKeyValidator
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.controller.AdminShell
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.AlertNotifier
import pitampoudel.komposeauth.core.utils.ServerUrl
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.storage.repository.StorageBucketRepository
import pitampoudel.komposeauth.user.service.FirstAdminService

@Controller
class AppConfigController(
    private val appConfigProvider: AppConfigProvider,
    private val appConfigService: AppConfigService,
    private val masterKeyValidator: MasterKeyValidator,
    private val adminShell: AdminShell,
    private val serverUrl: ServerUrl,
    private val emailService: EmailService,
    private val alertNotifier: AlertNotifier,
    private val phoneNumberVerificationService: PhoneNumberVerificationService,
    private val storageBucketRepository: StorageBucketRepository,
    private val startupRunner: StartupRunner,
    private val firstAdminService: FirstAdminService,
    val userContextService: UserContextService
) {

    @GetMapping("/admin/config")
    @Operation(summary = "web page to configure this app")
    fun form(
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        noStore(response)
        enforceConfigAccessOrLock(model = model, request = request, response = response)?.let { return it }
        return render(model, appConfigProvider.get())
    }

    /**
     * Saves one section, named by `section`, or clears it when `disconnect` is sent. Only that
     * section's fields are touched, so a form can never blank out a setting it didn't show.
     */
    @PostMapping("/admin/config")
    fun submit(
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        noStore(response)
        enforceConfigAccessOrLock(model = model, request = request, response = response)?.let { return it }
        // Carried in the forms so the next save is let in the way this request was.
        model.addAttribute("masterKey", postedKey(request)?.takeIf(masterKeyValidator::isValid))
        val current = appConfigProvider.get()
        if (request.getParameter(UNLOCK_PARAM) != null) return render(model, current)
        if (request.getParameter(FIRST_ADMIN_PARAM) != null) return createFirstAdmin(model, request, response, current)

        val section = request.getParameter(SECTION_PARAM)?.let(ConfigSections::byId)
            ?: return render(model, current)

        if (request.getParameter(DISCONNECT_PARAM) != null) {
            val saved = appConfigProvider.save(section.disconnect(current))
            afterSave(section, saved)
            model.addAttribute("saved", "${section.title} was disconnected.")
            return render(model, saved)
        }

        val posted = section.fields.associate { it.name to request.getParameter(it.name) }
            .filterValues { it != null }
        return when (val result = ConfigSections.apply(section, current, posted)) {
            is ConfigSections.Result.Saved -> {
                val saved = appConfigProvider.save(result.config)
                afterSave(section, saved)
                model.addAttribute("saved", "${section.title} saved.")
                // A settings section says so beside its own button; a connection, in the banner above its row.
                if (section.kind == ConfigSection.Kind.SETTINGS) model.addAttribute("savedSection", section.id)
                render(model, saved)
            }

            is ConfigSections.Result.Refused -> {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                model.addAttribute("error", result.problems.joinToString(". ") + ".")
                model.addAttribute("errorSection", section.id)
                render(model, current, draft = section.id to posted)
            }
        }
    }

    private fun createFirstAdmin(
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse,
        current: AppConfig
    ): String {
        val email = request.getParameter("adminEmail")
        val name = request.getParameter("adminName")
        when (val result = firstAdminService.create(email, name, request.getParameter("adminPassword"))) {
            is FirstAdminService.Result.Done -> model.addAttribute(
                "firstAdmin",
                if (result.created) "${result.user.email} is now a super admin. Sign in with it to manage this server."
                else "${result.user.email} already had an account and is now a super admin. Sign in with its own password."
            )

            is FirstAdminService.Result.Refused -> {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                model.addAttribute("firstAdminError", result.problem)
                model.addAttribute("adminEmail", email.orEmpty())
                model.addAttribute("adminName", name.orEmpty())
            }
        }
        return render(model, current)
    }

    /**
     * The Test button on a connection: sends one real message through it and says what happened.
     * Let in the same way as the page, so it works while the console is opened with the master key.
     */
    @PostMapping("/admin/config/test/{kind}")
    @ResponseBody
    fun test(
        @PathVariable kind: String,
        @RequestBody(required = false) body: TestRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<TestResponse> {
        noStore(response)
        if (!mayConfigure(request.getHeader(MASTER_KEY_HEADER))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(TestResponse(false, "Sign in as a super admin, or open this page with the master key."))
        }
        val to = body?.to?.trim().orEmpty()
        val outcome = when (kind) {
            "email" -> {
                if (to.isEmpty()) return badRequest("Enter an address to send the test to.")
                emailService.sendTestMail(serverUrl.value, to)
                    ?.let { TestResponse(false, it) }
                    ?: TestResponse(true, "Sent to $to. If it doesn't arrive in a minute, look in spam.")
            }

            "sms" -> {
                val phone = parsePhoneNumber(appConfigService.defaultPhoneRegion(), to)?.fullNumberInE164Format
                    ?: return badRequest("Enter a phone number, with its country code if it isn't local.")
                runCatching { phoneNumberVerificationService.initiate(phone) }.fold(
                    onSuccess = { message ->
                        if (message.message.startsWith("No verification provider")) {
                            TestResponse(false, "No provider is fully set up yet. Save one first.")
                        } else {
                            TestResponse(true, "A one-time code was sent to $phone.")
                        }
                    },
                    onFailure = { TestResponse(false, "The provider refused it: ${it.message ?: it.javaClass.simpleName}") }
                )
            }

            "alerts" -> alertNotifier.sendTest("✅ Alerts from ${appConfigService.getConfig().name ?: "your sign-in server"} will arrive in this channel.")
                ?.let { TestResponse(false, it) }
                ?: TestResponse(true, "Posted to the channel.")

            else -> return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(outcome)
    }

    @Serializable
    data class TestRequest(val to: String? = null)

    @Serializable
    data class TestResponse(val ok: Boolean, val message: String)

    private fun badRequest(message: String) =
        ResponseEntity.badRequest().body(TestResponse(false, message))

    private fun afterSave(section: ConfigSection, saved: AppConfig) {
        if (section.id == "errors") startupRunner.applyErrorReporting(saved.sentryDsn)
    }

    private fun render(model: Model, config: AppConfig, draft: Pair<String, Map<String, String?>>? = null): String {
        adminShell.apply(model)
        val context = ConfigContext(issuer = serverUrl.value)
        fun views(sections: List<ConfigSection>) = sections.map { section ->
            val view = ConfigSections.view(section, config, context)
            if (draft?.first != section.id) view
            else view.copy(fields = view.fields.map { field ->
                if (field.field.type == ConfigField.Type.SECRET) field
                else field.copy(value = draft.second[field.name] ?: field.value)
            })
        }
        model.addAttribute("connections", views(ConfigSections.connections))
        model.addAttribute("settings", views(ConfigSections.settings))
        model.addAttribute("storageReady", storageBucketRepository.findAll().any { it.primary })
        model.addAttribute("needsFirstAdmin", firstAdminService.isNeeded())
        model.addAttribute("viewerEmail", userContextService.authenticatedUserOrNull()?.email.orEmpty())
        return "admin/config"
    }

    /**
     * This page renders the server's settings and takes its secrets; keep it out of caches and history.
     *
     * No `Referrer-Policy: no-referrer` here: it makes the browser send `Origin: null` with the
     * page's own form post, which reads as a foreign origin.
     */
    private fun noStore(response: HttpServletResponse) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private")
        response.setHeader("Pragma", "no-cache")
    }

    private fun mayConfigure(suppliedKey: String?): Boolean {
        if (masterKeyValidator.isValid(suppliedKey)) return true
        val user = userContextService.authenticatedUserOrNull() ?: return false
        return user.roles.any { it == Roles.SUPER_ADMIN }
    }

    private fun enforceConfigAccessOrLock(
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String? {
        // There is deliberately no "no users yet, let anyone in" bootstrap here. This page writes
        // every secret the server holds — SMTP password, Twilio token, Google client secret — so
        // opening it to the internet for the window between deploy and first signup hands a fresh
        // instance to whoever finds it first. The operator already has BASE64_ENCRYPTION_KEY, which
        // is required to boot, so the master key below is always available to them for first-run.
        val suppliedKey = request.getHeader(MASTER_KEY_HEADER) ?: postedKey(request)
        if (masterKeyValidator.isValid(suppliedKey)) {
            return null
        }
        val user = userContextService.authenticatedUserOrNull()
        if (user != null) {
            if (!user.roles.any { it == Roles.SUPER_ADMIN }) {
                throw AccessDeniedException("Only super admins can access configuration.")
            }
            return null
        }
        if (suppliedKey != null) response.status = HttpServletResponse.SC_FORBIDDEN
        model.addAttribute("appName", appConfigProvider.get().name.orEmpty())
        model.addAttribute("keyRejected", suppliedKey != null)
        model.addAttribute("keyInAddress", request.queryString != null && request.getParameter(KEY_PARAM) != null)
        return "admin/config-locked"
    }

    /**
     * The key typed into this page's own form. Never one from the address, where it lands in
     * access logs, proxy logs, browser history and the Referer of whatever the page loads — and
     * since the servlet API hands back a posted field and a query parameter of the same name
     * alike, a request whose URL carries a query can't supply the key as a parameter at all.
     */
    private fun postedKey(request: HttpServletRequest): String? =
        request.takeIf { it.method == "POST" && it.queryString == null }?.getParameter(KEY_PARAM)

    companion object {
        const val MASTER_KEY_HEADER = "X-Master-Key"
        private const val KEY_PARAM = "key"

        /** Sent by the locked page: show the form for the key it carries, rather than save it. */
        private const val UNLOCK_PARAM = "unlock"

        /** Sent by the "Create the first admin" form, shown while nobody is a super admin. */
        const val FIRST_ADMIN_PARAM = "firstAdmin"

        /** Which [ConfigSection] a post saves. */
        const val SECTION_PARAM = "section"

        /** Sent by a connection's Disconnect button: clear every field of its section. */
        const val DISCONNECT_PARAM = "disconnect"
    }
}
