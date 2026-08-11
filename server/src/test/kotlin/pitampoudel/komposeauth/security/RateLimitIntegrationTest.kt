package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.security.ratelimit.RateLimitWindow
import pitampoudel.komposeauth.user.data.Credential

/**
 * The suite runs with `app.rate-limit.enabled=false`, because every test signs in from the same
 * client address and would otherwise spend one shared budget. This turns it back on, with a limit
 * small enough to reach, so the throttle is covered end to end rather than only in unit tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "app.rate-limit.enabled=true",
        "app.rate-limit.login.limit=3",
        "app.rate-limit.login.window=5m"
    ]
)
class RateLimitIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun clearCounters() {
        // Counters are shared state in Mongo, so start from a known budget.
        mongoTemplate.remove(org.springframework.data.mongodb.core.query.Query(), RateLimitWindow::class.java)
    }

    private fun attemptLogin(email: String, password: String, forwardedFor: String? = null) =
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            forwardedFor?.let { header("X-Forwarded-For", it) }
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = password)
            )
        }

    @Test
    fun `repeated failed sign-ins are throttled once the budget is spent`() {
        val email = "throttled@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)

        // Wrong password, so nothing succeeds by accident and each attempt spends budget.
        repeat(3) {
            attemptLogin(email, "WrongPassword1").andExpect {
                status { isForbidden() }
            }
        }

        attemptLogin(email, "WrongPassword1").andExpect {
            status { isTooManyRequests() }
            header { exists("Retry-After") }
        }
    }

    @Test
    fun `throttling applies to the endpoint regardless of whether the password was right`() {
        val email = "throttled-correct@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)

        repeat(3) {
            attemptLogin(email, "WrongPassword1").andExpect {
                status { isForbidden() }
            }
        }

        // Guessing correctly on the next attempt must not get through either — otherwise the limit
        // would only slow an attacker down until the moment they succeed.
        attemptLogin(email, "Password1").andExpect {
            status { isTooManyRequests() }
        }
    }

    /**
     * The bypass this configuration exists to close.
     *
     * `X-Forwarded-For` is chosen by whoever sends the request. With no proxy declared — the default,
     * and what the quickstart's directly-exposed container is — believing it would hand an attacker
     * a fresh budget per request simply for varying a header, which is to say no limit at all.
     */
    @Test
    fun `a forged forwarded-for header does not buy more attempts`() {
        val email = "spoofer@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)

        repeat(3) { attempt ->
            attemptLogin(email, "WrongPassword1", forwardedFor = "10.0.0.$attempt").andExpect {
                status { isForbidden() }
            }
        }

        attemptLogin(email, "WrongPassword1", forwardedFor = "10.0.0.99").andExpect {
            status { isTooManyRequests() }
        }
    }
}
