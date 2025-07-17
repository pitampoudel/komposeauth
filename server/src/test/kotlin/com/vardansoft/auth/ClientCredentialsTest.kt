package com.vardansoft.auth

import com.vardansoft.auth.oauth_clients.dto.toRegisteredClient
import com.vardansoft.auth.oauth_clients.entity.OAuth2Client
import com.vardansoft.auth.oauth_clients.repository.OAuth2ClientRepository
import com.vardansoft.auth.oauth_clients.repository.RegisteredOAuth2ClientRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(com.vardansoft.auth.config.TestConfig::class)
class ClientCredentialsTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var oauth2ClientRepository: OAuth2ClientRepository

    @Autowired
    private lateinit var registeredOAuth2ClientRepository: RegisteredOAuth2ClientRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var clientId: String
    private lateinit var clientSecret: String

    companion object {
        @Container
        @JvmStatic
        var mongo: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:5.0"))

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongo.replicaSetUrl }
        }
    }

    @BeforeEach
    fun setup() {
        // Generate client ID and secret
        clientId = UUID.randomUUID().toString()
        clientSecret = UUID.randomUUID().toString()

        // Create and save a test OAuth2 client
        val client = OAuth2Client(
            clientId = clientId,
            clientSecret = clientSecret,
            clientName = "Test Client",
            clientAuthenticationMethods = setOf(
                ClientAuthenticationMethod.CLIENT_SECRET_POST,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC
            ),
            authorizationGrantTypes = setOf(
                AuthorizationGrantType.AUTHORIZATION_CODE,
                AuthorizationGrantType.REFRESH_TOKEN,
                AuthorizationGrantType.CLIENT_CREDENTIALS
            ),
            redirectUris = setOf("http://localhost:8080/callback"),
            scopes = setOf(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL, "offline_access"),
            requireAuthorizationConsent = false,
            clientUri = null,
            logoUri = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Save the client to the repository
        oauth2ClientRepository.save(client)
    }

    @Test
    fun testClientCredentialsGrantType() {
        // Test that the client can be found by ID
        val client = oauth2ClientRepository.findById(clientId).orElse(null)
        assertEquals(clientId, client?.clientId)
        assertEquals("Test Client", client?.clientName)

        // Test that the client can be converted to a RegisteredClient
        val registeredClient = client?.toRegisteredClient()
        assertEquals(clientId, registeredClient?.clientId)
        assertEquals("Test Client", registeredClient?.clientName)

        // Test that the RegisteredOAuth2ClientRepository can find the client by ID
        val foundRegisteredClient = registeredOAuth2ClientRepository.findByClientId(clientId)
        assertEquals(clientId, foundRegisteredClient.clientId)
        assertEquals("Test Client", foundRegisteredClient.clientName)

        // Test that the client supports the REFRESH_TOKEN grant type
        assertTrue(client?.authorizationGrantTypes?.contains(AuthorizationGrantType.REFRESH_TOKEN) ?: false)

        // Test that client authentication works for token endpoint
        // Test client credentials grant type (which doesn't issue refresh tokens by default)
        mockMvc.perform(
            post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .param("scope", "openid profile email offline_access")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").exists())
            .andExpect(jsonPath("$.scope").exists())
        // Client credentials flow typically doesn't return refresh tokens
//            .andExpect(jsonPath("$.refresh_token").doesNotExist())
    }
}
