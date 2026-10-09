package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import pitampoudel.komposeauth.login.domain.AuthPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstallKomposeAuthTest {
    private val preferences = object : AuthPreferences {
        override val accessTokenPayload: Flow<String?> = emptyFlow()
        override suspend fun saveTokenData(tokenData: OAuth2Response) {}
        override fun tokenData() = OAuth2Response(accessToken = "access", expiresIn = 900)
        override suspend fun clear() {}
    }
    private val sent = mutableListOf<HttpRequestData>()

    private fun client(status: HttpStatusCode, setCookie: String? = null) = HttpClient(MockEngine { request ->
        sent += request
        respond("", status, setCookie?.let { headersOf(HttpHeaders.SetCookie, it) } ?: headersOf())
    }) {
        installKomposeAuth(preferences, "https://auth.example.com", listOf("https://api.example.com"))
    }

    @Test
    fun `the token goes to the auth and resource servers only`() = runBlocking {
        val client = client(HttpStatusCode.OK)

        client.get("https://auth.example.com/me")
        client.get("https://api.example.com/orders")
        client.get("https://cdn.example.net/logo.png")

        assertEquals(listOf("Bearer access", "Bearer access", null), sent.map { it.headers[HttpHeaders.Authorization] })
    }

    @Test
    fun `a 401 from another host is not retried with the token`() = runBlocking {
        client(HttpStatusCode.Unauthorized).get("https://cdn.example.net/logo.png")

        assertEquals(1, sent.size)
        assertNull(sent.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `only the auth server's cookies are kept and sent back`() = runBlocking {
        val auth = client(HttpStatusCode.OK, setCookie = "SESSION=one; Path=/")
        auth.get("https://auth.example.com/login-options")
        auth.post("https://auth.example.com/login")

        val other = client(HttpStatusCode.OK, setCookie = "TRACK=two; Path=/")
        other.get("https://cdn.example.net/a")
        other.get("https://cdn.example.net/b")

        assertEquals("SESSION=one", sent[1].headers[HttpHeaders.Cookie])
        assertNull(sent[3].headers[HttpHeaders.Cookie])
    }
}
