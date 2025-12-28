package pitampoudel.komposeauth.app_config

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AppConfigControllerSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `GET config is rejected when users exist and no auth and no master key`() {
        // Create a user to ensure countUsers() > 0.
        TestAuthHelpers.createUser(mockMvc, json, "config-test@example.com", password = "Password1")

        mockMvc.get("/config")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `GET config is allowed with valid master key even when users exist`() {
        // Create a user to ensure countUsers() > 0.
        TestAuthHelpers.createUser(mockMvc, json, "config-test@example.com", password = "Password1")
        val queryKey = TestConfig.testKey

        mockMvc.get("/config") {
            param("key", queryKey)
        }.andExpect {
            status { isOk() }
        }
    }
}
