package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pitampoudel.komposeauth.login.data.AuthClientImpl
import pitampoudel.komposeauth.login.domain.AuthPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignOutTest {
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
    private val requests = mutableListOf<HttpRequestData>()

    private val engine = MockEngine { request ->
        requests += request
        respond("""{"message":"Logout successful"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    private val httpClient = HttpClient(engine) {
        installKomposeAuth(preferences, "https://auth.example.com", listOf("https://api.example.com"))
    }
    private val handler = AuthStateHandler(preferences, AuthClientImpl(httpClient))

    @Test
    fun `signing out forgets the tokens and hands the refresh token over to be revoked`() = runBlocking {
        httpClient.get("https://api.example.com/orders")
        assertEquals("Bearer a1", requests.last().headers[HttpHeaders.Authorization])

        handler.logout()

        assertNull(preferences.stored)
        val logout = requests.single { it.url.encodedPath == "/logout" }
        val body = Json.parseToJsonElement(logout.body.toByteArray().decodeToString()).jsonObject
        assertEquals("r1", body["refreshToken"]?.jsonPrimitive?.content)

        httpClient.get("https://api.example.com/orders")
        assertNull(requests.last().headers[HttpHeaders.Authorization])
    }
}
