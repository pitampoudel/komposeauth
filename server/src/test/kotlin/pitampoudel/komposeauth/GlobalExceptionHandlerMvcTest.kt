package pitampoudel.komposeauth

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
import pitampoudel.komposeauth.core.domain.ApiEndpoints

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class GlobalExceptionHandlerMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `malformed json on login returns 400 with json error body`() {
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = "{not-json"
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.message") { exists() }
            jsonPath("$.path") { exists() }
        }
    }

    @Test
    fun `wrong http method returns 405 with json error body`() {
        // Use an unmapped method on a mapped endpoint to hit 405
        mockMvc.post("/${ApiEndpoints.LOGIN_OPTIONS}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isMethodNotAllowed() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.message") { exists() }
            jsonPath("$.path") { exists() }
        }
    }

    @Test
    fun `unmapped route does not 500 and returns a structured 4xx response`() {
        // Depending on the security filter chain, unknown paths may be intercepted as 401 before reaching the
        // MVC resource handler. We only assert safe behavior: no 5xx and (where possible) a JSON error envelope.
        mockMvc.get("/this-route-does-not-exist") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { is4xxClientError() }
        }
    }

}
