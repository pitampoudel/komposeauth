package pitampoudel.komposeauth.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.TestConfig
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The login page must not tell the browser where its own form may go.
 *
 * Password sign-in is a form POST whose response redirects, by way of `/oauth2/authorize`, to the
 * relying party's `redirect_uri` — another origin, and for a native client another scheme. Firefox
 * and Safari enforce `form-action` against every hop of that chain, so a `form-action 'self'` here
 * ends the sign-in silently: the POST is accepted, the session is established, and the redirect that
 * would carry the visitor back to their app never happens. It only shows for visitors who arrived
 * from a relying party, which is why it survived the direct sign-in this suite otherwise covers.
 *
 * Asserted as the absence of a directive rather than as a policy string, since the rest of the
 * policy is free to change.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class LoginPageContentSecurityPolicyTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `the login page does not restrict where its form may submit`() {
        val result = mockMvc.get("/session-login") {
            accept = MediaType.TEXT_HTML
        }.andExpect { status { isOk() } }.andReturn()

        val policy = assertNotNull(
            result.response.getHeader("Content-Security-Policy"),
            "the login page carried no content security policy at all"
        )

        assertFalse(
            policy.contains("form-action"),
            "form-action constrains the redirect that completes an authorization request, so the " +
                    "policy must not name it — was: $policy"
        )
        // The directives that do not stand in the way of a redirect are still expected to be there,
        // so that dropping one is a deliberate act rather than a side effect of this test.
        assertTrue(policy.contains("frame-ancestors 'none'"), "was: $policy")
        assertTrue(policy.contains("object-src 'none'"), "was: $policy")
    }
}
