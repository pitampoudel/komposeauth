package pitampoudel.komposeauth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Platform

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class AuthApplicationTests {

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
