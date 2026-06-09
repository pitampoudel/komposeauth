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

private val OIDC_INSTANT_CLAIMS = setOf("iat", "exp", "auth_time", "nbf", "updated_at")
private const val METADATA_CLAIMS_KEY = "metadata.token.claims"

enum class OidcClaimTypeMode {
    // Spring's OIDC accessors cast standard timestamp claims to Instant.
    SPRING_ACCESSORS,
    // Nimbus/Gson token serialization cannot reflect into java.time.Instant.
    SERIALIZATION
}

internal fun fixOidcClaimTypes(claims: MutableMap<String, Any>): MutableMap<String, Any> {
    claims.replaceAll { key, value ->
        when (value) {
            is Number -> if (key in OIDC_INSTANT_CLAIMS) Instant.ofEpochSecond(value.toLong()) else value
            is String -> if (key in OIDC_INSTANT_CLAIMS) value.toInstantClaimOrNull() ?: value else value
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                fixOidcClaimTypes(value.toMutableMap() as MutableMap<String, Any>)
            }
            is List<*> -> value.map { item ->
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    fixOidcClaimTypes(item.toMutableMap() as MutableMap<String, Any>)
                } else {
                    item
                }
            }
            else -> value
        }
    }
    return claims
}

private fun String.toInstantClaimOrNull(): Instant? {
    toLongOrNull()?.let { return Instant.ofEpochSecond(it) }
    return runCatching { Instant.parse(this) }.getOrNull()
}

internal fun serializeOidcClaimTypes(claims: MutableMap<String, Any>): MutableMap<String, Any> {
    claims.replaceAll { _, value ->
        when (value) {
            // Keep claim maps JSON-friendly before token generation.
            is Instant -> value.epochSecond
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                serializeOidcClaimTypes(value.toMutableMap() as MutableMap<String, Any>)
            }
            is List<*> -> value.map { item ->
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    serializeOidcClaimTypes(item.toMutableMap() as MutableMap<String, Any>)
                } else {
                    item
                }
            }
            else -> value
        }
    }
    return claims
}

@Suppress("UNCHECKED_CAST")
private fun fixMetadataClaimTypes(
    metadata: MutableMap<String, Any>,
    claimTypeMode: OidcClaimTypeMode
): MutableMap<String, Any> {
    (metadata[METADATA_CLAIMS_KEY] as? Map<*, *>)?.let {
        val claims = it.toMutableMap() as MutableMap<String, Any>
        metadata[METADATA_CLAIMS_KEY] = when (claimTypeMode) {
            OidcClaimTypeMode.SPRING_ACCESSORS -> fixOidcClaimTypes(claims)
            OidcClaimTypeMode.SERIALIZATION -> serializeOidcClaimTypes(claims)
        }
    }
    return metadata
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
    objectMapper: ObjectMapper,
    claimTypeMode: OidcClaimTypeMode = OidcClaimTypeMode.SERIALIZATION
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
            it.putAll(doc.accessTokenMetadata?.let { m ->
                fixMetadataClaimTypes(objectMapper.readValue(m, mapTypeRef), claimTypeMode)
            } ?: emptyMap())
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
        val idToken = OidcIdToken(
            doc.oidcIdTokenValue,
            doc.oidcIdTokenIssuedAt,
            doc.oidcIdTokenExpiresAt,
            doc.oidcIdTokenClaims?.let {
                val claims = objectMapper.readValue(it, mapTypeRef)
                when (claimTypeMode) {
                    OidcClaimTypeMode.SPRING_ACCESSORS -> fixOidcClaimTypes(claims)
                    OidcClaimTypeMode.SERIALIZATION -> serializeOidcClaimTypes(claims)
                }
            } ?: mutableMapOf()
        )
        builder.token(idToken) {
            it.putAll(doc.oidcIdTokenMetadata?.let { m ->
                fixMetadataClaimTypes(objectMapper.readValue(m, mapTypeRef), claimTypeMode)
            } ?: emptyMap())
        }
    }

    return builder.build()
}