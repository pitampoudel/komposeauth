package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.user.data.Credential
import java.time.Instant
import java.util.Base64
import kotlin.test.assertEquals

/**
 * With `spring.security.oauth2.authorizationserver.issuer` set, nothing a caller writes into the Host
 * or forwarded headers reaches a token's issuer, and a bearer is held to that issuer.
 */
@SpringBootTest(properties = ["spring.security.oauth2.authorizationserver.issuer=${ConfiguredIssuerIntegrationTest.ISSUER}"])
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class ConfiguredIssuerIntegrationTest {

    companion object {
        const val ISSUER = "https://auth.example.test"
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var json: Json
    @Autowired private lateinit var jwtEncoder: JwtEncoder

    private val forgedHost = RequestPostProcessor { request ->
        request.serverName = "attacker.example"
        request
    }

    @Test
    fun `discovery names the configured issuer`() {
        val body = mockMvc.get("/.well-known/openid-configuration") { with(forgedHost) }
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        assertEquals(ISSUER, json.parseToJsonElement(body).jsonObject["issuer"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a token from login carries the configured issuer`() {
        val email = "issuer-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)

        val body = mockMvc.post("/${ApiEndpoints.LOGIN}") {
            with(forgedHost)
            param("responseType", ResponseType.TOKEN.name)
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(Credential.UsernamePassword(username = email, password = "Password1"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString

        val accessToken = json.parseToJsonElement(body).jsonObject.getValue("access_token").jsonPrimitive.content
        val claims = json.parseToJsonElement(String(Base64.getUrlDecoder().decode(accessToken.split(".")[1]))).jsonObject
        assertEquals(ISSUER, claims["iss"]?.jsonPrimitive?.content)
        assertEquals(200, me(accessToken))
    }

    @Test
    fun `a token this server signed under another issuer is refused`() {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "other-issuer@example.com")
        val now = Instant.now()
        val foreign = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer("https://attacker.example")
                    .subject(userId)
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(600))
                    .claim("scope", listOf("openid"))
                    .build()
            )
        ).tokenValue
        assertEquals(401, me(foreign))
    }

    private fun me(accessToken: String) =
        mockMvc.get("/${ApiEndpoints.ME}") { header("Authorization", "Bearer $accessToken") }.andReturn().response.status
}
