package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.core.security.csrf.CrossOriginCsrfTokenRepository
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The route a browser app on another origin has to take, end to end, over the real token repository.
 *
 * Such an app cannot read the CSRF cookie — the same-origin policy forbids it — so if `/csrf` does
 * not work, then requiring a token means it can never make an authenticated write at all. That makes
 * this the test that says the protection is usable rather than merely present.
 *
 * It needs a context of its own. `SecurityMockMvcRequestPostProcessors.csrf()`, which
 * [TestConfig] applies throughout the suite, does not just add a token: it replaces the token
 * repository on the shared filter chain with a session-backed stand-in, permanently and for every
 * later test in the same context. Nothing here may trigger that, so every request below opts out and
 * signs in by hand rather than through the usual helper — and the unique property gives this class
 * its own context so no other test can have swapped the repository out first.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
@TestPropertySource(properties = ["komposeauth.test.context=csrf-token-endpoint"])
class CsrfTokenEndpointIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    /** As [TestAuthHelpers.loginCookie], but without the post-processor that swaps the repository. */
    private fun loginCookie(email: String, password: String = "Password1") =
        mockMvc.post("/${ApiEndpoints.LOGIN}") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            param("responseType", ResponseType.COOKIE.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(
                Credential.UsernamePassword(username = email, password = password)
            )
        }.andExpect {
            status { isOk() }
        }.andReturn().response.getCookie(ACCESS_TOKEN_COOKIE_NAME)
            ?: error("login did not return an access-token cookie")

    @Test
    fun `a browser app can fetch a token and spend it`() {
        val email = "csrf-endpoint@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val accessCookie = loginCookie(email)

        val primed = mockMvc.get("/csrf") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }.andReturn().response

        val issued = json.parseToJsonElement(primed.contentAsString).jsonObject
        val token = issued.getValue("token").jsonPrimitive.content
        val headerName = issued.getValue("headerName").jsonPrimitive.content
        val csrfCookie = primed.getCookie(CrossOriginCsrfTokenRepository.COOKIE_NAME)

        assertTrue(token.isNotBlank(), "expected a usable token, got '$token'")
        assertNotNull(csrfCookie, "expected the CSRF cookie to be set alongside the token")

        // A token is only worth anything if the protection then accepts it.
        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(accessCookie, csrfCookie)
            header(headerName, token)
            content = """{"givenName":"CrossOrigin"}"""
        }.andExpect {
            status { isOk() }
        }

        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertEquals("CrossOrigin", user.firstName)
    }

    @Test
    fun `the token endpoint is reachable before signing in`() {
        // The first write a browser app makes may be the sign-in itself, so this cannot require
        // authentication.
        mockMvc.get("/csrf") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `the issued token is not cacheable`() {
        // It is per-browser authority; a shared cache handing one visitor's token to another would
        // defeat the point of issuing it.
        mockMvc.get("/csrf") {
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            header { string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")) }
        }
    }
}
