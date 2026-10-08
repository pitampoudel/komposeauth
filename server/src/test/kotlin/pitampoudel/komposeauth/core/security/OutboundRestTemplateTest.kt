package pitampoudel.komposeauth.core.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.TestConfig
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class OutboundRestTemplateTest {

    @Autowired
    private lateinit var restTemplate: RestTemplate

    /** Twilio's API takes form posts; the shared client is built by Boot now, not `RestTemplate()`. */
    @Test
    fun `the shared RestTemplate can post a form`() {
        assertTrue(restTemplate.messageConverters.any { it is FormHttpMessageConverter })
    }
}
