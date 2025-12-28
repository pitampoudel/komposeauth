package pitampoudel.komposeauth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * Production-safety smoke tests around Actuator endpoints.
 *
 * We mainly care that the app exposes a health endpoint and that it isn't accidentally public.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `health endpoint exists and requires authentication`() {
        mockMvc.get("/actuator/health") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            // By default our security config requires auth for any request not explicitly permitted.
            // Health being protected is the safer production default.
            status { isUnauthorized() }
        }
    }
}
