package pitampoudel.komposeauth.oauth_clients.cimd

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Literal addresses resolve without DNS, so the refusal is checked without any network. */
class HttpClientMetadataFetcherTest {

    private val fetcher = HttpClientMetadataFetcher()

    @Test
    fun `addresses on private and shared networks are never fetched`() {
        for (host in listOf(
            "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254", "[fd00::1]",
            "0.1.2.3", "100.64.0.1", "100.127.255.254"
        )) {
            val refused = assertFailsWith<InvalidClientMetadata>(host) { fetcher.fetch(URI("https://$host/client")) }
            assertTrue(refused.message.orEmpty().contains("private network"), "$host: ${refused.message}")
        }
    }
}
