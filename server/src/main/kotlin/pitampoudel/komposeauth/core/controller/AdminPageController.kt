package pitampoudel.komposeauth.core.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository
import pitampoudel.komposeauth.user.service.UserService

/**
 * One thing the server can or cannot currently do, named the way an operator would name it.
 */
data class ConsoleCheck(
    val name: String,
    val ready: Boolean,
    val detail: String
)

/**
 * The admin console's pages.
 *
 * Every section lives under `/admin`, is rendered into `admin/layout.html`, and keeps its data
 * work on the server where the page can state plainly what is on and what is not.
 */
@Controller
@RequestMapping("/admin")
class AdminPageController(
    private val adminShell: AdminShell,
    private val appConfigService: AppConfigService,
    private val userService: UserService,
    private val oAuth2ClientRepository: OAuth2ClientRepository
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    fun overview(model: Model): String {
        adminShell.apply(model)

        val config = appConfigService.getConfig()
        val userCount = userService.countUsers()
        val clientCount = oAuth2ClientRepository.count()
        val roles = userService.listRoles()

        model.addAttribute("postureLine", postureLine(userCount, clientCount))
        model.addAttribute("signInMethods", signInMethods(config))
        model.addAttribute("deliveryChannels", deliveryChannels(config))
        model.addAttribute("roles", roles)
        model.addAttribute("hasCustomRoles", roles.any { !it.builtIn })
        return "admin/overview"
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    fun users(model: Model): String {
        adminShell.apply(model)
        return "admin/users"
    }

    @GetMapping("/clients")
    @PreAuthorize("hasRole('ADMIN')")
    fun clients(model: Model): String {
        adminShell.apply(model)
        return "admin/clients"
    }

    /** The opening line of the overview: who can get in, and what can ask on their behalf. */
    private fun postureLine(userCount: Long, clientCount: Long): String {
        val people = "$userCount ${if (userCount == 1L) "person" else "people"}"
        val apps = "$clientCount ${if (clientCount == 1L) "app" else "apps"}"
        return when {
            userCount == 0L -> "Nobody has signed in yet. The first person through is yours to manage."
            clientCount == 0L -> "$people can sign in. No app is registered to ask for tokens yet."
            else -> "$people can sign in, and $apps can ask for tokens on their behalf."
        }
    }

    private fun isSet(value: String?) = !value.isNullOrBlank()

    private fun signInMethods(config: AppConfig): List<ConsoleCheck> = listOf(
        ConsoleCheck(
            name = "Password",
            ready = true,
            detail = "Built in. Anyone with an email address or phone number on their account."
        ),
        ConsoleCheck(
            name = "Google",
            ready = isSet(config.googleAuthClientId),
            detail = if (isSet(config.googleAuthClientId)) {
                "Signing in through the Google client set under OAuth."
            } else {
                "Add a Google client ID under OAuth to turn this on."
            }
        ),
        ConsoleCheck(
            name = "Apple",
            ready = isSet(config.appleAuthClientId),
            detail = if (isSet(config.appleAuthClientId)) {
                "Signing in through the Apple client set under OAuth."
            } else {
                "Add an Apple client ID under OAuth to turn this on."
            }
        ),
        ConsoleCheck(
            name = "Passkeys",
            ready = isSet(config.rpId),
            detail = if (isSet(config.rpId)) {
                "Registered against ${config.rpId}."
            } else {
                "Set a relying party ID under Support & Platform to turn this on."
            }
        ),
        ConsoleCheck(
            name = "One-time code by text",
            ready = isSet(config.smsProvider),
            detail = if (isSet(config.smsProvider)) {
                "Codes go out through ${providerName(config.smsProvider)}."
            } else {
                "Choose an SMS provider to turn this on."
            }
        )
    )

    private fun deliveryChannels(config: AppConfig): List<ConsoleCheck> = listOf(
        ConsoleCheck(
            name = "Email",
            ready = isSet(config.smtpHost),
            detail = if (isSet(config.smtpHost)) {
                "Sent through ${config.smtpHost}."
            } else {
                "Verification links and password resets cannot go out until SMTP is set."
            }
        ),
        ConsoleCheck(
            name = "Text messages",
            ready = isSet(config.smsProvider),
            detail = if (isSet(config.smsProvider)) {
                "Sent through ${providerName(config.smsProvider)}."
            } else {
                "No provider is selected, so one-time codes cannot be sent."
            }
        )
    )

    private fun providerName(provider: String?): String = when (provider?.lowercase()) {
        "twilio" -> "Twilio"
        "samaye" -> "Samaye"
        "sparrow" -> "Sparrow"
        else -> provider.orEmpty()
    }
}
