package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.authorization.OAuth2AuthorizationDocumentRepository
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.repository.OneTimeTokenRepository

/**
 * Ends the ways an account stays signed in: its browser sessions here, its refresh tokens, and the
 * authorizations relying apps refresh against. An access token already issued is a signed JWT and
 * stays valid until it expires; nothing here can recall it.
 */
@Service
class AccessRevocation(
    private val authorizationRepository: OAuth2AuthorizationDocumentRepository,
    private val oneTimeTokenRepository: OneTimeTokenRepository,
    private val sessionRepository: FindByIndexNameSessionRepository<out Session>
) {

    /** A session keeps the roles it signed in with, so it has to end when those change. */
    fun endSessions(userId: ObjectId, except: String? = null) {
        sessionRepository.findByPrincipalName(userId.toHexString()).keys
            .filterNot { it == except }
            .forEach(sessionRepository::deleteById)
    }

    fun revokeAll(userId: ObjectId) {
        endSessions(userId)
        authorizationRepository.deleteAllByPrincipalName(userId.toHexString())
        oneTimeTokenRepository.deleteAllByUserIdAndPurposeIn(
            userId,
            listOf(OneTimeToken.Purpose.REFRESH_TOKEN, OneTimeToken.Purpose.RESET_PASSWORD)
        )
    }
}
