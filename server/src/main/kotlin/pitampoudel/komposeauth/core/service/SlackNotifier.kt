package pitampoudel.komposeauth.core.service

import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Service
class SlackNotifier(
    private val restClient: RestClient,
    private val appConfigService: AppConfigService
) {
    suspend fun send(text: String) {
        val url = appConfigService.getConfig().slackWebhookUrl ?: return
        restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("text" to text))
            .retrieve()
            .toBodilessEntity()
    }
}
