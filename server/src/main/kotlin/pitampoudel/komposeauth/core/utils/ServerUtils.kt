package pitampoudel.komposeauth.core.utils

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.stereotype.Component
import java.net.URI

/**
 * This server's public address: every token's issuer, every emailed link, the KYC callback, the
 * Google sign-in's redirect URI and the API docs' server are built from it, and the authorization
 * server publishes it in its metadata. It is `spring.security.oauth2.authorizationserver.issuer`,
 * which the authorization server reads too.
 *
 * It is required, and nothing is ever read off a request in its place: a request's Host and
 * forwarded headers are whatever the caller wrote, so a reset link built from them could be made to
 * point at somebody else's server. Without it the server does not start.
 */
@Component
class ServerUrl(settings: AuthorizationServerSettings) {

    /** `scheme://host[:port]`, exactly as configured and as relying parties compare `iss`. */
    val value: String = validated(settings.issuer)

    override fun toString(): String = value

    companion object {
        const val PROPERTY = "spring.security.oauth2.authorizationserver.issuer"
        const val ENV_VAR = "SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER"

        /**
         * An origin and nothing else. Every link is this plus a path the server serves at its root,
         * so a path here would name pages that are not there; a trailing slash would make `iss` a
         * different string from the one relying parties were told to expect; and a query or a
         * fragment has no place in an issuer (RFC 8414 §2).
         */
        fun validated(issuer: String?): String {
            if (issuer.isNullOrBlank()) {
                throw IllegalStateException(
                    "$PROPERTY is not set. It is this server's public address, the issuer every token " +
                        "carries and the base of every emailed link, and it is never guessed from a " +
                        "request. Set it, usually as the environment variable $ENV_VAR, to the address " +
                        "your apps reach this server at, e.g. https://auth.example.com"
                )
            }
            fun refuse(why: String): Nothing = throw IllegalStateException(
                "$PROPERTY is '$issuer', which $why. It must be an origin alone, written exactly as " +
                    "your apps check the issuer, e.g. https://auth.example.com (set as $ENV_VAR)"
            )

            val uri = runCatching { URI(issuer) }.getOrElse { refuse("is not a URL") }
            if (uri.scheme != "http" && uri.scheme != "https") refuse("is not an http:// or https:// address")
            if (uri.rawAuthority == null || uri.host.isNullOrEmpty()) refuse("names no host")
            if (uri.rawUserInfo != null) refuse("carries a user name")
            if (uri.rawPath == "/") refuse("ends with a slash")
            if (!uri.rawPath.isNullOrEmpty()) refuse("has a path")
            if (uri.rawQuery != null || issuer.endsWith("?")) refuse("has a query")
            if (uri.rawFragment != null || issuer.endsWith("#")) refuse("has a fragment")
            return issuer
        }
    }
}

/** The one spelling an email address is stored and looked up under. */
fun String.normalizedEmail(): String = trim().lowercase()
