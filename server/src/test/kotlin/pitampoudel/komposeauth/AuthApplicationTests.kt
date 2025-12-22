package pitampoudel.komposeauth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Testcontainers
import pitampoudel.komposeauth.core.data.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Platform

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthApplicationTests  : MongoContainerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `login options endpoint returns ok`() {
        mockMvc.get("/${ApiEndpoints.LOGIN_OPTIONS}") {
            param("platform", Platform.WEB.name)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun contextLoads() {
    }
}
