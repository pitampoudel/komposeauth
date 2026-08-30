package pitampoudel.komposeauth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * Production-safety smoke tests around Actuator endpoints.
 *
 * Health is deliberately the one unauthenticated corner of Actuator. A container platform probes it
 * before anything has signed in and with no credentials it could offer, so behind authentication it
 * answers 401 and the probe reads that as a dead instance — which on Cloud Run means falling back to
 * a TCP check against the port, and the port is bound partway through startup. What is given up by
 * publishing it is bounded here: a bare status, no component breakdown, and nothing else under
 * `/actuator` reachable at all.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `readiness probe answers without credentials`() {
        mockMvc.get("/actuator/health/readiness") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `liveness probe answers without credentials`() {
        mockMvc.get("/actuator/health/liveness") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `health reports a bare status and never the components behind it`() {
        val body = mockMvc.get("/actuator/health") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString

        // The component breakdown names the datastore, the mail host and the disk beneath them, and
        // this endpoint is public. `show-details`/`show-components` keep it to the status alone.
        assert(!body.contains("components")) { "health response leaked components: $body" }
        assert(!body.contains("mongo")) { "health response leaked datastore detail: $body" }
    }

    @Test
    fun `the rest of actuator stays behind authentication`() {
        // Neither exposed over HTTP nor permitted by the security chain. Either alone would do; the
        // test is here so that widening `management.endpoints.web.exposure.include` cannot quietly
        // publish the environment, which on this server holds every provider secret it has.
        listOf("/actuator/env", "/actuator/configprops", "/actuator/beans").forEach { path ->
            mockMvc.get(path) {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}
