package pitampoudel.komposeauth.core.service

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Service
class SlackNotifier(
    private val restClient: RestClient,
    private val appConfigService: AppConfigService
) {
    private val log = LoggerFactory.getLogger(SlackNotifier::class.java)
    fun send(text: String) {
        val config = appConfigService.getConfig()
        val botToken = config.slackBotToken?.takeIf { it.isNotBlank() } ?: run {
            log.debug("Slack bot token not configured; skipping notification")
            return
        }
        val channelId = config.slackChannelId?.takeIf { it.isNotBlank() } ?: run {
            log.debug("Slack channel ID not configured; skipping notification")
            return
        }

        runCatching { post(botToken, channelId, text) }.onFailure { throwable ->
            log.warn("Failed to send Slack notification", throwable)
        }
    }

    /** Posts [text] and answers why Slack refused it, or null once it is in the channel. */
    fun sendTest(text: String): String? {
        val config = appConfigService.getConfig()
        val botToken = config.slackBotToken?.takeIf { it.isNotBlank() } ?: return "No bot token is saved"
        val channelId = config.slackChannelId?.takeIf { it.isNotBlank() } ?: return "No channel ID is saved"
        return runCatching { post(botToken, channelId, text) }.fold(
            onSuccess = { response ->
                // Slack answers 200 with ok=false for a bad token or a channel the bot isn't in.
                if (response?.ok == true) null
                else "Slack said: ${response?.error ?: "no answer"}"
            },
            onFailure = { it.message ?: it.javaClass.simpleName }
        )
    }

    private fun post(botToken: String, channelId: String, text: String): SlackResponse? =
        restClient.post()
            .uri("https://slack.com/api/chat.postMessage")
            .header("Authorization", "Bearer $botToken")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("channel" to channelId, "text" to text))
            .retrieve()
            .body(SlackResponse::class.java)

    @Serializable
    data class SlackResponse(val ok: Boolean = false, val error: String? = null)
}
