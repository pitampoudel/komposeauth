package pitampoudel.komposeauth.user.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.SlackNotifier
import pitampoudel.komposeauth.user.entity.User

@Service
class RoleChangeEmailNotifier(
    private val emailService: EmailService,
    private val appConfigService: AppConfigService,
    private val slackNotifier: SlackNotifier,
) {
    enum class Action { GRANTED, REVOKED }

    /**
     * Best-effort: returns whether the email send call succeeded.
     * Never throws.
     */
    fun notify(target: User, action: Action, actor: String?): Boolean {
        val actorLabel = actor?.takeIf { it.isNotBlank() } ?: "system"
        val targetLabel = target.email ?: target.phoneNumber ?: target.id.toHexString()

        when (action) {
            Action.GRANTED -> slackNotifier.send("🛡️ Admin role granted to $targetLabel by $actorLabel")
            Action.REVOKED -> slackNotifier.send("🛡️ Admin role revoked for $targetLabel by $actorLabel")
        }

        val to = target.email
        if (to.isNullOrBlank()) return false

        val appName = appConfigService.getConfig().name ?: "our app"
        val websiteUrl = appConfigService.getConfig().websiteUrl ?: ""

        val subject = when (action) {
            Action.GRANTED -> "Admin access granted"
            Action.REVOKED -> "Admin access revoked"
        }


        val message = when (action) {
            Action.GRANTED -> "You’ve been granted <b>ADMIN</b> access in $appName by ${actor ?: "an admin"}."
            Action.REVOKED -> "Your <b>ADMIN</b> access in $appName was revoked by ${actor ?: "an admin"}."
        }

        return emailService.sendHtmlMail(
            baseUrl = websiteUrl,
            to = to,
            subject = subject,
            template = "email/generic",
            model = mapOf(
                "recipientName" to target.firstNameOrUser(),
                "message" to message,
                "actionUrl" to websiteUrl.takeIf { it.isNotBlank() },
                "actionText" to if (websiteUrl.isNotBlank()) "Open $appName" else null,
                "actionMessage" to "If you didn’t expect this, contact support."
            )
        )
    }
}
