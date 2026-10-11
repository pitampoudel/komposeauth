package pitampoudel.komposeauth.user.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.user.entity.User

/**
 * Tells the account holder that a verified email address or phone number was replaced, at the place
 * they could still be reached before: whoever replaced it may not be them. Best-effort: each returns
 * whether the mail went out.
 */
@Service
class ContactChangeNotifier(
    private val emailService: EmailService,
    private val appConfigService: AppConfigService,
) {
    /** Sent to [previous], the address that was replaced. */
    fun emailReplaced(user: User, previous: String): Boolean =
        send(user, to = previous, what = "email address", replacement = user.email)

    /**
     * Sent to the account's verified email address, if it has one. The replaced number is not
     * messaged: the SMS providers here deliver verification codes, not free text.
     */
    fun phoneNumberReplaced(user: User, previous: String): Boolean {
        val to = user.verifiedEmail() ?: return false
        return send(user, to = to, what = "phone number", replacement = user.phoneNumber, previous = previous)
    }

    private fun send(user: User, to: String, what: String, replacement: String?, previous: String? = null): Boolean {
        val appName = appConfigService.getConfig().name ?: "our app"
        val websiteUrl = appConfigService.getConfig().websiteUrl ?: ""
        val from = previous?.let { " from $it" }.orEmpty()
        return emailService.sendHtmlMail(
            baseUrl = websiteUrl,
            to = to,
            subject = "Your $what was changed",
            template = "email/generic",
            model = mapOf(
                "recipientName" to user.firstNameOrUser(),
                "message" to "The $what on your $appName account was changed$from to $replacement.",
                "actionUrl" to websiteUrl.takeIf { it.isNotBlank() },
                "actionText" to if (websiteUrl.isNotBlank()) "Open $appName" else null,
                "actionMessage" to "If you didn’t make this change, contact support right away."
            )
        )
    }
}
