package pitampoudel.komposeauth.core.service

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.net.URI

/**
 * Tells the operators' chat that something worth a look happened. Goes to an incoming webhook (Slack,
 * Discord, Mattermost, Google Chat, Rocket.Chat and anything else that takes `{"text": …}`), to a Slack
 * app's channel, or to both, as the configuration names them. With neither, nothing is sent.
 */
@Service
class AlertNotifier(
    private val restClient: RestClient,
    private val appConfigService: AppConfigService
) {
    private val log = LoggerFactory.getLogger(AlertNotifier::class.java)

    fun send(text: String) {
        val config = appConfigService.getConfig()
        destinations(config).forEach { destination ->
            destination.post(text)?.let { problem -> log.warn("Alert not delivered to {}: {}", destination.name, problem) }
        }
    }

    /** Posts [text] everywhere alerts go and answers what refused it, or null once every one took it. */
    fun sendTest(text: String): String? {
        val config = appConfigService.getConfig()
        val all = destinations(config)
        if (all.isEmpty()) {
            return if (!config.slackBotToken.isNullOrBlank() || !config.slackChannelId.isNullOrBlank()) {
                "The Slack app needs both a bot token and a channel ID"
            } else "No webhook URL or Slack app is saved"
        }
        return all.mapNotNull { destination -> destination.post(text)?.let { "${destination.name}: $it" } }
            .joinToString("; ")
            .ifEmpty { null }
    }

    private fun destinations(config: AppConfig): List<Destination> = listOfNotNull(
        config.alertWebhookUrl?.takeIf { it.isNotBlank() }?.let { Webhook(it) },
        slackApp(config)
    )

    private fun slackApp(config: AppConfig): Destination? {
        val token = config.slackBotToken?.takeIf { it.isNotBlank() } ?: return null
        val channel = config.slackChannelId?.takeIf { it.isNotBlank() } ?: return null
        return SlackApp(token, channel)
    }

    private sealed interface Destination {
        val name: String

        /** Null once delivered; otherwise why not, in words an operator can act on. */
        fun post(text: String): String?
    }

    private inner class Webhook(private val url: String) : Destination {
        private val host = runCatching { URI(url).host }.getOrNull()
        override val name = "Webhook" + (host?.let { " to $it" } ?: "")

        override fun post(text: String): String? {
            if (host == null || !url.startsWith("https://") && !url.startsWith("http://")) {
                return "the webhook URL isn't an http(s) address"
            }
            // Discord names the message `content`; the others share Slack's `text`.
            val discord = host == "discord.com" || host.endsWith(".discord.com") || host == "discordapp.com"
            return runCatching {
                restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(if (discord) DiscordMessage(text) else TextMessage(text))
                    .retrieve()
                    .toBodilessEntity()
            }.fold(
                onSuccess = { null },
                onFailure = { refusal(it) }
            )
        }
    }

    private inner class SlackApp(private val token: String, private val channel: String) : Destination {
        override val name = "Slack app"

        override fun post(text: String): String? = runCatching {
            restClient.post()
                .uri("https://slack.com/api/chat.postMessage")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(SlackMessage(channel, text))
                .retrieve()
                .body(SlackResponse::class.java)
        }.fold(
            // Slack answers 200 with ok=false for a bad token or a channel the bot isn't in.
            onSuccess = { response -> if (response?.ok == true) null else "Slack said: ${response?.error ?: "no answer"}" },
            onFailure = { refusal(it) }
        )
    }

    private fun refusal(error: Throwable): String = when (error) {
        is RestClientResponseException -> "answered ${error.statusCode.value()}"
        else -> error.message ?: error.javaClass.simpleName
    }

    @Serializable
    data class TextMessage(val text: String)

    @Serializable
    data class DiscordMessage(val content: String)

    @Serializable
    data class SlackMessage(val channel: String, val text: String)

    @Serializable
    data class SlackResponse(val ok: Boolean = false, val error: String? = null)
}
