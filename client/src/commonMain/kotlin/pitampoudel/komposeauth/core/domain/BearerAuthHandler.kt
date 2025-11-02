package pitampoudel.komposeauth.core.domain

import io.ktor.client.plugins.auth.AuthConfig

internal interface BearerAuthHandler {
    val authUrl: String
    val serverUrls: List<String>
    fun configure(auth: AuthConfig)
}