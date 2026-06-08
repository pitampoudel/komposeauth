package pitampoudel.komposeauth.authorization

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant
import java.util.Base64

private val mapTypeRef = object : TypeReference<MutableMap<String, Any>>() {}

// JWT/OIDC claims whose values are epoch-second timestamps. Jackson deserializes JSON
// numbers into Double for Map<String, Any>, but Spring's OidcIdToken expects Instant.
private val TIMESTAMP_CLAIM_NAMES = setOf("iat", "exp", "auth_time", "nbf", "updated_at")

private fun fixOidcClaimTypes(claims: MutableMap<String, Any>): MutableMap<String, Any> {
    for (key in TIMESTAMP_CLAIM_NAMES) {
        val value = claims[key]
        if (value is Number) {
            claims[key] = Instant.ofEpochSecond(value.toLong())
        }
    }
    return claims
}

// Use Java serialization for attributes: these contain Spring Security objects
// (OAuth2AuthorizationRequest, UsernamePasswordAuthenticationToken, etc.) that include
// final classes which Jackson's default-typing skips, causing them to come back as
// LinkedHashMap and breaking PKCE / redirect-URI validation.
private fun serializeAttributes(attributes: Map<String, Any>): String {
    val baos = ByteArrayOutputStream()
    ObjectOutputStream(baos).use { it.writeObject(attributes) }
    return Base64.getEncoder().encodeToString(baos.toByteArray())
}

@Suppress("UNCHECKED_CAST")
private fun deserializeAttributes(encoded: String): Map<String, Any> {
    val bytes = Base64.getDecoder().decode(encoded)
    return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as Map<String, Any> }
}

fun toOAuth2AuthorizationDocument(auth: OAuth2Authorization, objectMapper: ObjectMapper): OAuth2AuthorizationDocument {
    val authCode = auth.getToken(OAuth2AuthorizationCode::class.java)
    val accessToken = auth.getToken(OAuth2AccessToken::class.java)
    val refreshToken = auth.refreshToken
    val idToken = auth.getToken(OidcIdToken::class.java)

    return OAuth2AuthorizationDocument(
        id = auth.id,
        registeredClientId = auth.registeredClientId,
        principalName = auth.principalName,
        authorizationGrantType = auth.authorizationGrantType.value,
        authorizedScopes = auth.authorizedScopes,
        state = auth.getAttribute(OAuth2ParameterNames.STATE),
        attributes = serializeAttributes(auth.attributes),

        authorizationCodeValue = authCode?.token?.tokenValue,
        authorizationCodeIssuedAt = authCode?.token?.issuedAt,
        authorizationCodeExpiresAt = authCode?.token?.expiresAt,
        authorizationCodeMetadata = authCode?.metadata?.let { objectMapper.writeValueAsString(it) },

        accessTokenValue = accessToken?.token?.tokenValue,
        accessTokenIssuedAt = accessToken?.token?.issuedAt,
        accessTokenExpiresAt = accessToken?.token?.expiresAt,
        accessTokenMetadata = accessToken?.metadata?.let { objectMapper.writeValueAsString(it) },
        accessTokenType = accessToken?.token?.tokenType?.value,
        accessTokenScopes = accessToken?.token?.scopes,

        refreshTokenValue = refreshToken?.token?.tokenValue,
        refreshTokenIssuedAt = refreshToken?.token?.issuedAt,
        refreshTokenExpiresAt = refreshToken?.token?.expiresAt,
        refreshTokenMetadata = refreshToken?.metadata?.let { objectMapper.writeValueAsString(it) },

        oidcIdTokenValue = idToken?.token?.tokenValue,
        oidcIdTokenIssuedAt = idToken?.token?.issuedAt,
        oidcIdTokenExpiresAt = idToken?.token?.expiresAt,
        oidcIdTokenMetadata = idToken?.metadata?.let { objectMapper.writeValueAsString(it) },
        oidcIdTokenClaims = idToken?.token?.claims?.let { objectMapper.writeValueAsString(it) },
    )
}

 fun fromOAuth2AuthorizationDocument(
    doc: OAuth2AuthorizationDocument,
    registeredClient: RegisteredClient,
    objectMapper: ObjectMapper
): OAuth2Authorization? {

    val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
        .id(doc.id)
        .principalName(doc.principalName)
        .authorizationGrantType(AuthorizationGrantType(doc.authorizationGrantType))
        .authorizedScopes(doc.authorizedScopes)

    doc.attributes?.let { builder.attributes { attrs -> attrs.putAll(deserializeAttributes(it)) } }
    doc.state?.let { builder.attribute(OAuth2ParameterNames.STATE, it) }

    if (doc.authorizationCodeValue != null) {
        val code = OAuth2AuthorizationCode(
            doc.authorizationCodeValue, doc.authorizationCodeIssuedAt, doc.authorizationCodeExpiresAt
        )
        builder.token(code) {
            it.putAll(doc.authorizationCodeMetadata?.let { m ->
                objectMapper.readValue(
                    m,
                    mapTypeRef
                )
            } ?: emptyMap())
        }
    }

    if (doc.accessTokenValue != null) {
        val token = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            doc.accessTokenValue,
            doc.accessTokenIssuedAt,
            doc.accessTokenExpiresAt,
            doc.accessTokenScopes ?: emptySet()
        )
        builder.token(token) {
            it.putAll(doc.accessTokenMetadata?.let { m -> objectMapper.readValue(m, mapTypeRef) } ?: emptyMap())
        }
    }

    if (doc.refreshTokenValue != null) {
        val token = OAuth2RefreshToken(
            doc.refreshTokenValue, doc.refreshTokenIssuedAt, doc.refreshTokenExpiresAt
        )
        builder.token(token) {
            it.putAll(doc.refreshTokenMetadata?.let { m -> objectMapper.readValue(m, mapTypeRef) } ?: emptyMap())
        }
    }

    if (doc.oidcIdTokenValue != null) {
        val claims = doc.oidcIdTokenClaims
            ?.let { fixOidcClaimTypes(objectMapper.readValue(it, mapTypeRef)) }
            ?: mutableMapOf()
        val idToken = OidcIdToken(
            doc.oidcIdTokenValue, doc.oidcIdTokenIssuedAt, doc.oidcIdTokenExpiresAt, claims
        )
        builder.token(idToken) {
            it.putAll(doc.oidcIdTokenMetadata?.let { m -> objectMapper.readValue(m, mapTypeRef) } ?: emptyMap())
        }
    }

    return builder.build()
}