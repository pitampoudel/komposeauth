package pitampoudel.komposeauth.app_config

import kotlinx.serialization.json.Json
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.app_config.controller.AppConfigController
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AppConfigControllerSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var appConfigProvider: AppConfigProvider

    @Test
    fun `GET config without a session or a key asks for the key and shows nothing else`() {
        // Create a user to ensure countUsers() > 0.
        TestAuthHelpers.createUser(mockMvc, json, "config-test-1@example.com", password = "Password1")

        mockMvc.get("/admin/config")
            .andExpect {
                status { isOk() }
                content { string(containsString("Master key")) }
                content { string(not(containsString("Save configuration"))) }
            }
    }

    @Test
    fun `GET config is allowed with valid master key header even when users exist`() {
        // Create a user to ensure countUsers() > 0.
        TestAuthHelpers.createUser(mockMvc, json, "config-test-2@example.com", password = "Password1")

        mockMvc.get("/admin/config") {
            header(AppConfigController.MASTER_KEY_HEADER, TestConfig.testKey)
        }.andExpect {
            status { isOk() }
            content { string(containsString("Save configuration")) }
        }
    }

    /**
     * A key in the address lands in access logs, proxy logs, browser history and Referer headers, so
     * the page refuses to read it from there, even when it is the right key.
     */
    @Test
    fun `the master key is not accepted from the address`() {
        mockMvc.get("/admin/config?key={key}", TestConfig.testKey)
            .andExpect {
                status { isOk() }
                content { string(containsString("not read from the address")) }
                content { string(not(containsString("Save configuration"))) }
            }

        mockMvc.post("/admin/config?key={key}", TestConfig.testKey) {
            param("unlock", "true")
        }.andExpect {
            content { string(not(containsString("Save configuration"))) }
        }

        val before = appConfigProvider.get().name
        mockMvc.post("/admin/config?key={key}", TestConfig.testKey) {
            param("name", "Saved through the address")
        }.andExpect {
            content { string(not(containsString("Configuration saved."))) }
        }
        assertEquals(before, appConfigProvider.get().name)
    }

    @Test
    fun `a wrong key is refused`() {
        mockMvc.post("/admin/config") {
            param("unlock", "true")
            param("key", "not-the-key")
        }.andExpect {
            status { isForbidden() }
            content { string(containsString("That is not the master key.")) }
            content { string(not(containsString("Save configuration"))) }
        }
    }

    /** The page's own form: the key typed into the lock page opens it, and rides along to the save. */
    @Test
    fun `the master key posted in the form opens the page and carries on to the save`() {
        mockMvc.post("/admin/config") {
            param("unlock", "true")
            param("key", TestConfig.testKey)
        }.andExpect {
            status { isOk() }
            content { string(containsString("Save configuration")) }
            content { string(containsString("""name="key" value="${TestConfig.testKey}"""")) }
            content { string(not(containsString("Configuration saved."))) }
        }

        val before = appConfigProvider.get()
        try {
            mockMvc.post("/admin/config") {
                param("key", TestConfig.testKey)
                param("name", "Saved through the form")
            }.andExpect {
                status { isOk() }
                content { string(containsString("Configuration saved.")) }
            }
            assertEquals("Saved through the form", appConfigProvider.get().name)
        } finally {
            appConfigProvider.save(before)
        }
    }
}
