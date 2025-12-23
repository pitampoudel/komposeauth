package pitampoudel.komposeauth.webauthn

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class WebAuthnControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var appConfigService: AppConfigService

    @BeforeEach
    fun setUp() {
        appConfigService.appConfigProvider.save(AppConfig(googleAuthClientId = "test-client-id"))
    }

    @AfterEach
    fun tearDown() {
        appConfigService.appConfigProvider.clearCache()
    }

    @Test
    fun `get login options returns configuration`() {
        mockMvc.get("/${ApiEndpoints.LOGIN_OPTIONS}?platform=${Platform.WEB.name}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.googleClientId") { value("test-client-id") }
                jsonPath("$.publicKeyAuthOptionsJson") { exists() }
            }
        }
    }

    @Test
    fun `get webauthn register options succeeds for authenticated user`() {
        val (_, cookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "webauthn-admin@example.com")

        mockMvc.post("/webauthn/register/options") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.challenge") { exists() }
                jsonPath("$.rp") { exists() }
            }
        }
    }
}
