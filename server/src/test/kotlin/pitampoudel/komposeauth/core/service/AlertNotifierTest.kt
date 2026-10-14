package pitampoudel.komposeauth.core.service

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertNotifierTest {

    private val builder = RestClient.builder().configureMessageConverters {
        it.registerDefaults().withKotlinSerializationJsonConverter(KotlinSerializationJsonHttpMessageConverter(Json))
    }
    private val server = MockRestServiceServer.bindTo(builder).build()

    private fun notifier(config: AppConfig) = AlertNotifier(
        builder.build(),
        AppConfigService(mock<AppConfigProvider>().also { whenever(it.get()).thenReturn(config) })
    )

    @Test
    fun `a webhook gets the message as text, which Slack, Mattermost and Google Chat read`() {
        server.expect(requestTo("https://hooks.slack.com/services/T/B/x"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"text":"hello"}""", org.springframework.test.json.JsonCompareMode.STRICT))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        assertNull(notifier(AppConfig(alertWebhookUrl = "https://hooks.slack.com/services/T/B/x")).sendTest("hello"))
        server.verify()
    }

    @Test
    fun `a Discord webhook gets it as content, the only field Discord reads`() {
        server.expect(requestTo("https://discord.com/api/webhooks/1/abc"))
            .andExpect(content().json("""{"content":"hello"}""", org.springframework.test.json.JsonCompareMode.STRICT))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))

        assertNull(notifier(AppConfig(alertWebhookUrl = "https://discord.com/api/webhooks/1/abc")).sendTest("hello"))
        server.verify()
    }

    @Test
    fun `a Slack app still works, and alongside a webhook both are posted to`() {
        server.expect(requestTo("https://chat.example.com/hooks/1"))
            .andRespond(withSuccess())
        server.expect(requestTo("https://slack.com/api/chat.postMessage"))
            .andExpect(header("Authorization", "Bearer xoxb-1"))
            .andExpect(content().json("""{"channel":"C1","text":"hello"}"""))
            .andRespond(withSuccess("""{"ok":true}""", MediaType.APPLICATION_JSON))

        val config = AppConfig(alertWebhookUrl = "https://chat.example.com/hooks/1", slackBotToken = "xoxb-1", slackChannelId = "C1")
        assertNull(notifier(config).sendTest("hello"))
        server.verify()
    }

    @Test
    fun `the test says which destination refused and why`() {
        server.expect(requestTo("https://chat.example.com/hooks/1")).andRespond(withStatus(HttpStatus.NOT_FOUND))

        val problem = notifier(AppConfig(alertWebhookUrl = "https://chat.example.com/hooks/1")).sendTest("hello")
        assertEquals("Webhook to chat.example.com: answered 404", problem)
    }

    @Test
    fun `nothing configured is said plainly, and a half-set Slack app is named`() {
        assertEquals("No webhook URL or Slack app is saved", notifier(AppConfig()).sendTest("hello"))
        assertTrue(notifier(AppConfig(slackChannelId = "C1")).sendTest("hello")!!.contains("bot token"))
    }

    @Test
    fun `a failed alert never fails the action that raised it`() {
        server.expect(requestTo("https://chat.example.com/hooks/1")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
        notifier(AppConfig(alertWebhookUrl = "https://chat.example.com/hooks/1")).send("hello")
        server.verify()
    }
}
