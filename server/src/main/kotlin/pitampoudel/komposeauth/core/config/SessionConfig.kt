package pitampoudel.komposeauth.core.config

import org.mongodb.spring.session.MongoIndexedSessionRepository
import org.mongodb.spring.session.config.annotation.web.http.EnableMongoHttpSession
import org.springframework.boot.web.server.autoconfigure.ServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.session.config.SessionRepositoryCustomizer

@Configuration
@EnableMongoHttpSession
class SessionConfig {

    /**
     * Makes the session store honour `server.servlet.session.timeout`.
     *
     * Without this the timeout is whatever `@EnableMongoHttpSession` says, and its default —
     * `MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS`, thirty minutes — is not reachable from
     * configuration at all: the annotation takes an `int` literal, so no property or environment
     * variable can move it. Spring Boot's own session auto-configuration would have wired the
     * property in, but it only recognises the `org.springframework.session.data.mongo` repository
     * and backs off in the presence of the one the annotation registers, so nothing here was
     * reading the property. A deployment that set it saw no effect.
     *
     * Thirty idle minutes is short for a server whose sessions have to survive a trip out to an
     * identity provider and back. The session is where the pending authorization request lives —
     * its `state`, its PKCE verifier, and the relying party's own request waiting to be resumed —
     * so a visitor who lingers at Google's account chooser for longer than that returns to a server
     * with no memory of having sent them, and the callback fails with
     * `authorization_request_not_found`. See the handler for that in `WebSecurityConfig`.
     */
    @Bean
    fun sessionTimeoutCustomizer(
        serverProperties: ServerProperties
    ): SessionRepositoryCustomizer<MongoIndexedSessionRepository> =
        SessionRepositoryCustomizer { repository ->
            serverProperties.servlet.session.timeout?.let { repository.setDefaultMaxInactiveInterval(it) }
        }
}
