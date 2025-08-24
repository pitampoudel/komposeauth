package com.vardansoft.authx.authorizations.service

import com.vardansoft.authx.authorizations.repository.OAuth2AuthorizationRepository
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service

@Service
class MongoOAuth2AuthorizationService(
    private val authorizationRepository: OAuth2AuthorizationRepository,
    private val registeredClientRepository: RegisteredClientRepository
) : OAuth2AuthorizationService {

    override fun save(authorization: OAuth2Authorization) {
        authorizationRepository.save(authorization.toEntity())
    }

    override fun remove(authorization: OAuth2Authorization) {
        authorizationRepository.deleteById(authorization.id)
    }

    override fun findById(id: String): OAuth2Authorization? {
        return authorizationRepository.findById(id)
            .map {
                it.toObject(
                    registeredClient = registeredClientRepository.findById(
                        it.registeredClientId
                    ) ?: throw IllegalStateException("Registered client not found for id: ${it.registeredClientId}"),
                )
            }
            .orElse(null)
    }

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val document = when (tokenType?.value) {
            "state" -> authorizationRepository.findByState(token)
            "code" -> authorizationRepository.findByAuthorizationCodeValue(token)
            "access_token" -> authorizationRepository.findByAccessTokenValue(token)
            "refresh_token" -> authorizationRepository.findByRefreshTokenValue(token)
            else -> authorizationRepository.findByTokenValue(token)
        }
        return document?.let {
            it.toObject(
                registeredClient = registeredClientRepository.findById(
                    it.registeredClientId
                ) ?: throw IllegalStateException("Registered client not found for id: ${it.registeredClientId}"),
            )
        }
    }


}
