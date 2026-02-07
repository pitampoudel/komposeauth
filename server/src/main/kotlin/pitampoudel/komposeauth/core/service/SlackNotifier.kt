package pitampoudel.komposeauth.core.service

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

        runCatching {
            restClient.post()
                .uri("https://slack.com/api/chat.postMessage")
                .header("Authorization", "Bearer $botToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("channel" to channelId, "text" to text))
                .retrieve()
                .toBodilessEntity()
        }.onFailure { throwable ->
            log.warn("Failed to send Slack notification", throwable)
        }
    }
}
