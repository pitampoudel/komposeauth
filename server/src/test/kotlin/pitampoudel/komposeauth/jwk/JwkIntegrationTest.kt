package pitampoudel.komposeauth.jwk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.jwk.entity.Jwk
import pitampoudel.komposeauth.jwk.repository.JwkRepository
import pitampoudel.komposeauth.jwk.service.JwkService
import java.security.interfaces.RSAPublicKey
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class JwkIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jwkRepository: JwkRepository
    @Autowired private lateinit var jwtEncoder: JwtEncoder
    @Autowired private lateinit var jwkService: JwkService

    private val objectMapper = ObjectMapper()

    @Test
    fun `first startup creates jwk record`() {
        val all = jwkRepository.findAll()
        assertTrue(all.isNotEmpty())
        assertTrue(all.any { it.kid == "spring-boot-jwk" })

        val doc = all.first { it.kid == "spring-boot-jwk" }
        assertTrue(doc.publicKeyPem.contains("BEGIN PUBLIC KEY"))
        assertTrue(doc.privateKeyPem.startsWith("enc:"))
        assertNotNull(doc.createdAt)
    }

    @Test
    fun `kid uniqueness is enforced`() {
        val first = Jwk(
            kid = "unique-kid",
            publicKeyPem = "-----BEGIN PUBLIC KEY-----\nZm9v\n-----END PUBLIC KEY-----",
            privateKeyPem = "enc:deadbeef"
        )
        jwkRepository.save(first)

        val second = first.copy(id = org.bson.types.ObjectId())
        assertThrows(DuplicateKeyException::class.java) {
            jwkRepository.save(second)
        }
    }

    @Test
    fun `jwks endpoint is public and exposes only public params`() {
        val res = mockMvc.get("/oauth2/jwks").andExpect {
            status { isOk() }
        }.andReturn().response

        val json = res.contentAsString
        val node: JsonNode = objectMapper.readTree(json)

        val keys = node.get("keys")
        assertTrue(keys.isArray)
        assertTrue(keys.size() >= 1)
        val key0 = keys[0]

        assertEquals("RSA", key0.get("kty").asText())
        assertTrue(key0.hasNonNull("kid"))
        assertTrue(key0.hasNonNull("n"))
        assertTrue(key0.hasNonNull("e"))

        // Ensure private parameters are not published
        assertFalse(key0.has("d"))
        assertFalse(key0.has("p"))
        assertFalse(key0.has("q"))
        assertFalse(key0.has("dp"))
        assertFalse(key0.has("dq"))
        assertFalse(key0.has("qi"))
    }

    @Test
    fun `jwt minted by encoder verifies using jwks response`() {
        val claims = JwtClaimsSet.builder()
            .issuer("test")
            .subject("user-1")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("scope", "read")
            .build()

        val jwt: Jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims))

        val signed = SignedJWT.parse(jwt.tokenValue)
        val kid: String? = signed.header.keyID

        val jwksJson = mockMvc.get("/oauth2/jwks").andReturn().response.contentAsString
        val jwkSet = JWKSet.parse(jwksJson)

        val rsa = jwkSet.keys
            .filterIsInstance<RSAKey>()
            .firstOrNull { kid == null || it.keyID == kid }
            ?: throw IllegalStateException("No RSA JWK found matching kid='$kid'")

        val publicKey: RSAPublicKey = rsa.toRSAPublicKey()

        val decoder = NimbusJwtDecoder.withPublicKey(publicKey).build()
        val decoded = decoder.decode(jwt.tokenValue)

        assertEquals("user-1", decoded.subject)
    }

    @Test
    fun `fails closed when stored private key cannot be decrypted into valid RSA key`() {
        val current = jwkRepository.findAll().firstOrNull { it.kid == "spring-boot-jwk" }
            ?: throw IllegalStateException("Expected an existing spring-boot-jwk record")

        jwkRepository.deleteAll(jwkRepository.findAll().filter { it.kid == "spring-boot-jwk" })
        jwkRepository.save(
            Jwk(
                kid = "spring-boot-jwk",
                publicKeyPem = current.publicKeyPem,
                privateKeyPem = "enc:%%%" // encrypted-looking but invalid
            )
        )

        assertThrows(Exception::class.java) {
            jwkService.loadOrCreateKeyPair()
        }
    }
}
