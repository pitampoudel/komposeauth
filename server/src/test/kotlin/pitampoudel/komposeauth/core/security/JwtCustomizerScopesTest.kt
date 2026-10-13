package pitampoudel.komposeauth.core.security

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import java.util.UUID
import kotlin.test.assertEquals

/**
 * What a token says about the person: an app the person had to consent to learns what the scopes
 * they consented to cover, as from userinfo, and an app registered here learns everything it did.
 */
class JwtCustomizerScopesTest {

    private val user = User(
        id = ObjectId.get(),
        firstName = "Asha",
        lastName = "Rai",
        email = "asha@example.com",
        emailVerified = true,
        phoneNumber = "+9779812345678",
        phoneNumberVerified = true,
        picture = "https://example.com/asha.png"
    )

    private val customizer = WebAuthorizationConfig().jwtCustomizer(
        userService = mock<UserService>().also { whenever(it.findByUserName(user.id.toHexString())).thenReturn(user) },
        kycService = mock<KycService>().also { whenever(it.isVerified(any())).thenReturn(false) }
    )

    private val personalClaims = setOf(
        "email", "emailVerified", "givenName", "familyName", "picture", "kycVerified", "phoneNumberVerified"
    )

    private fun client(requireConsent: Boolean) = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://example.com/callback")
        .scopes { it.addAll(setOf(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL, OidcScopes.PHONE)) }
        .clientSettings(ClientSettings.builder().requireAuthorizationConsent(requireConsent).build())
        .build()

    private fun personalClaimsFor(requireConsent: Boolean, scopes: Set<String>): Set<String> {
        val claims = JwtClaimsSet.builder().subject(user.id.toHexString())
        val context = JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
            .registeredClient(client(requireConsent))
            .principal(UsernamePasswordAuthenticationToken.authenticated(user.id.toHexString(), null, emptyList()))
            .authorizedScopes(scopes)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .build()
        customizer.customize(context)
        return claims.build().claims.keys.intersect(personalClaims)
    }

    @Test
    fun `an app the person consented to learns only what its scopes cover`() {
        assertEquals(emptySet(), personalClaimsFor(requireConsent = true, scopes = setOf(OidcScopes.OPENID)))
        assertEquals(
            setOf("email", "emailVerified"),
            personalClaimsFor(requireConsent = true, scopes = setOf(OidcScopes.OPENID, OidcScopes.EMAIL))
        )
        assertEquals(
            setOf("givenName", "familyName", "picture", "kycVerified"),
            personalClaimsFor(requireConsent = true, scopes = setOf(OidcScopes.OPENID, OidcScopes.PROFILE))
        )
        assertEquals(
            setOf("phoneNumberVerified"),
            personalClaimsFor(requireConsent = true, scopes = setOf(OidcScopes.OPENID, OidcScopes.PHONE))
        )
    }

    @Test
    fun `an app registered here learns everything, whatever it asked for`() {
        assertEquals(personalClaims, personalClaimsFor(requireConsent = false, scopes = setOf(OidcScopes.OPENID)))
    }
}
