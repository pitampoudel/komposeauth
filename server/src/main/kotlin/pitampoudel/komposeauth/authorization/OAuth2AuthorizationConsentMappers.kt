package pitampoudel.komposeauth.authorization

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent

fun OAuth2AuthorizationConsent.toDocument() = OAuth2AuthorizationConsentDocument(
    id = consentId(registeredClientId, principalName),
    registeredClientId = registeredClientId,
    principalName = principalName,
    authorities = authorities.mapNotNull { it.authority }.toSet(),
)

fun OAuth2AuthorizationConsentDocument.toConsent(): OAuth2AuthorizationConsent =
    OAuth2AuthorizationConsent.withId(registeredClientId, principalName)
        .authorities { it.addAll(authorities.map(::SimpleGrantedAuthority)) }
        .build()
