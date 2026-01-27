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
        val url = appConfigService.getConfig().slackWebhookUrl ?: run {
            log.debug("Slack webhook URL not configured; skipping notification")
            return
        }
        runCatching {
            restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("text" to text))
                .retrieve()
                .toBodilessEntity()
        }.onFailure { throwable ->
            log.warn("Failed to send Slack notification", throwable)
        }
    }
}
