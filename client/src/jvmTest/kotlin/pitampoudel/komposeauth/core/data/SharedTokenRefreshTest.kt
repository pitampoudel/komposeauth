package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import pitampoudel.komposeauth.login.domain.AuthPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A refresh token answers once, so two clients built on the same stored tokens must not both redeem
 * it: the second one to try is a replay to the server, which ends the sign-in for both.
 */
class SharedTokenRefreshTest {
    private val preferences = object : AuthPreferences {
        var stored: OAuth2Response? = OAuth2Response(accessToken = "a1", refreshToken = "r1", expiresIn = 900)
        override val accessTokenPayload: Flow<String?> = emptyFlow()
        override suspend fun saveTokenData(tokenData: OAuth2Response) {
            stored = tokenData
        }
        override fun tokenData() = stored
        override suspend fun clear() {
            stored = null
        }
    }
    private var refreshes = 0

    private val engine = MockEngine { request ->
        when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/login" -> {
                refreshes++
                respond(
                    """{"access_token":"a2","refresh_token":"r2","expires_in":900}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            request.url.host == "api.example.com" && request.headers[HttpHeaders.Authorization] == "Bearer a1" ->
                respond("", HttpStatusCode.Unauthorized)
            else -> respond("", HttpStatusCode.OK)
        }
    }

    private fun client() = HttpClient(engine) {
        installKomposeAuth(preferences, "https://auth.example.com", listOf("https://api.example.com"))
    }

    @Test
    fun `a client whose token another client already refreshed takes the stored one`() = runBlocking {
        val first = client()
        val second = client()
        // Both have loaded the first pair of tokens.
        second.get("https://auth.example.com/me")

        assertEquals(HttpStatusCode.OK, first.get("https://api.example.com/orders").status)
        assertEquals(HttpStatusCode.OK, second.get("https://api.example.com/orders").status)

        assertEquals(1, refreshes)
        assertEquals("r2", preferences.stored?.refreshToken)
    }

    @Test
    fun `a refresh after the user signed out stores nothing`() = runBlocking {
        val client = client()
        client.get("https://auth.example.com/me")
        preferences.clear()

        assertEquals(HttpStatusCode.Unauthorized, client.get("https://api.example.com/orders").status)

        assertEquals(0, refreshes)
        assertEquals(null, preferences.stored)
    }
}
