package pitampoudel.komposeauth.oauth_clients.cimd

import org.apache.hc.client5.http.DnsResolver
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Literal addresses resolve without DNS, so the refusal is checked without any network. */
class HttpClientMetadataFetcherTest {

    private val fetcher = HttpClientMetadataFetcher()

    /** Answers the n-th lookup with the n-th answer, and every later one with the last. */
    private class ChangingDns(vararg answers: List<String>) : DnsResolver {
        private val answers = answers.map { addresses -> addresses.map { InetAddress.getByName(it) }.toTypedArray() }
        val lookups = AtomicInteger()

        override fun resolve(host: String): Array<InetAddress> = answers[minOf(lookups.getAndIncrement(), answers.lastIndex)]
        override fun resolveCanonicalHostname(host: String) = host
    }

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

    @Test
    fun `a name is refused when any of its addresses is private`() {
        val dns = ChangingDns(listOf("203.0.113.10", "10.0.0.1"))

        val refused = assertFailsWith<InvalidClientMetadata> {
            HttpClientMetadataFetcher(dns).fetch(URI("https://cimd.example/client"))
        }
        assertTrue(refused.message.orEmpty().contains("private network"), "${refused.message}")
    }

    @Test
    fun `the connection goes to the address that was checked, whatever the name resolves to next`() {
        ServerSocket(0, 50, InetAddress.getLoopbackAddress()).use { internal ->
            val reached = AtomicInteger()
            thread(isDaemon = true) {
                runCatching { while (true) internal.accept().use { reached.incrementAndGet() } }
            }
            // A public address while the name is checked, then the server's own network for whoever
            // asks again: what a rebinding name answers. 203.0.113.0/24 is for documentation, so the
            // checked answer is tried and goes nowhere.
            val dns = ChangingDns(listOf("203.0.113.10"), listOf("127.0.0.1"))
            val fetcher = HttpClientMetadataFetcher(dns, timeout = Duration.ofMillis(300))

            assertFails { fetcher.fetch(URI("https://cimd.example:${internal.localPort}/client")) }

            assertEquals(1, dns.lookups.get())
            assertEquals(0, reached.get())
        }
    }
}
