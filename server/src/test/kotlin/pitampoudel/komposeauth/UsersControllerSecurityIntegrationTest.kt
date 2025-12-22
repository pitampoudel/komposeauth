package pitampoudel.komposeauth

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.core.domain.ApiEndpoints

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class UsersControllerSecurityIntegrationTest {

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
