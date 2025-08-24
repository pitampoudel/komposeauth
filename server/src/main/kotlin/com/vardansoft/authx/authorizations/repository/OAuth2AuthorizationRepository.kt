package com.vardansoft.authx.authorizations.repository

import com.vardansoft.authx.authorizations.entity.OAuth2AuthorizationEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OAuth2AuthorizationRepository : MongoRepository<OAuth2AuthorizationEntity, String> {

    fun findByState(state: String): OAuth2AuthorizationEntity?

    fun findByAuthorizationCodeValue(authorizationCode: String): OAuth2AuthorizationEntity?

    fun findByAccessTokenValue(accessToken: String): OAuth2AuthorizationEntity?

    fun findByRefreshTokenValue(refreshToken: String): OAuth2AuthorizationEntity?

    @Query(
        "{ \$or: [ " +
                "{ 'authorizationCodeValue': ?0 }, " +
                "{ 'accessTokenValue': ?0 }, " +
                "{ 'refreshTokenValue': ?0 }, " +
                "{ 'oidcIdTokenValue': ?0 }, " +
                "{ 'userCodeValue': ?0 }, " +
                "{ 'deviceCodeValue': ?0 } " +
                "] }"
    )
    fun findByTokenValue(tokenValue: String): OAuth2AuthorizationEntity?
}