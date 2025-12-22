package pitampoudel.komposeauth

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Testcontainers
import pitampoudel.komposeauth.core.data.ApiEndpoints

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UsersControllerSecurityIntegrationTest : MongoContainerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var json: Json

    @Test
    fun `GET users requires admin or read any user scope - normal user gets 403`() {
        TestAuthHelpers.createUser(mockMvc, json, "normal1@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "normal1@example.com")

        mockMvc.get("/${ApiEndpoints.USERS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET stats requires admin or read any user scope - normal user gets 403`() {
        TestAuthHelpers.createUser(mockMvc, json, "normal2@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "normal2@example.com")

        mockMvc.get("/${ApiEndpoints.STATS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

}
