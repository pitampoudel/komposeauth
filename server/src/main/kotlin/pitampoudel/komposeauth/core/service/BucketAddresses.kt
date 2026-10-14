package pitampoudel.komposeauth.core.service

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * How one bucket's addresses are written and read.
 *
 * Built from where the bucket is and never from how to sign in to it, so every store's addresses are
 * known even when its client is never built: a store whose files were copied elsewhere still
 * recognises the addresses on record without a credential for its own cloud.
 */
interface BucketAddresses {
    /**
     * The object [address] names in this bucket, or null when it is not one of this bucket's.
     *
     * Matched against the whole address: one that only contains an address of ours names somewhere
     * else. A name with a `.` or `..` segment is refused too, since a URL normaliser would read it as
     * climbing out of its folder and open some other object than the one the name says.
     */
    fun nameOf(address: String): String?

    /** The address this bucket hands out for [name], without the version an upload puts on the end. */
    fun addressOf(name: String): String

    /** Whether [text] mentions an address in this bucket anywhere, inside longer text included. */
    fun mentionedIn(text: String): Boolean

    /** Every form of address this bucket recognises for [name]. */
    fun formsOf(name: String): List<String>

    companion object {
        /** Whether either bucket would claim an address of the other's, so the two can't both be listed. */
        fun overlap(a: BucketAddresses, b: BucketAddresses): Boolean {
            val probe = "komposeauth-overlap/probe"
            return a.formsOf(probe).any { b.nameOf(it) != null } || b.formsOf(probe).any { a.nameOf(it) != null }
        }

        internal fun isPlainName(name: String): Boolean =
            name.isNotEmpty() && name.split('/').none { it == "." || it == ".." }

        /** One encoded path segment per segment of [name], as [URLDecoder] reads them back. */
        fun encodePath(name: String): String =
            name.split('/').joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8).replace("+", "%20") }
    }
}

/** Addresses that are a fixed base followed by the name: a bucket's own host, an endpoint, or a CDN. */
internal class UnderBase(base: String) {
    val prefix: String = base.trimEnd('/') + "/"

    fun nameOf(address: String): String? {
        if (!address.startsWith(prefix) || address.any(Char::isWhitespace)) return null
        val path = address.substring(prefix.length).substringBefore('#').substringBefore('?')
        val name = try {
            path.split('/').joinToString("/") { URLDecoder.decode(it, Charsets.UTF_8) }
        } catch (_: IllegalArgumentException) {
            return null
        }
        return name.takeIf(BucketAddresses::isPlainName)
    }

    fun addressOf(name: String): String = prefix + BucketAddresses.encodePath(name)

    fun mentionedIn(text: String): Boolean = text.contains(prefix)
}

/**
 * An S3 bucket's addresses: `https://{bucket}.s3.{region}.amazonaws.com/{name}` on AWS, or
 * `{endpoint}/{bucket}/{name}` (path-style) on any other service speaking the S3 API. With a
 * [publicBaseUrl], new files are handed out as `{publicBaseUrl}/{name}` and both forms are read.
 */
class S3Addresses(
    bucket: String,
    region: String,
    endpoint: String? = null,
    publicBaseUrl: String? = null
) : BucketAddresses {
    private val bucketBase = UnderBase(endpoint?.let { "${it.trimEnd('/')}/$bucket" } ?: "https://$bucket.s3.$region.amazonaws.com")
    private val publicBase = publicBaseUrl?.let(::UnderBase)

    override fun nameOf(address: String): String? = publicBase?.nameOf(address) ?: bucketBase.nameOf(address)

    override fun addressOf(name: String): String = (publicBase ?: bucketBase).addressOf(name)

    override fun mentionedIn(text: String): Boolean = bucketBase.mentionedIn(text) || publicBase?.mentionedIn(text) == true

    override fun formsOf(name: String): List<String> = listOfNotNull(publicBase?.addressOf(name), bucketBase.addressOf(name))
}

/**
 * A GCS bucket's addresses: the media link an upload answers, `https://storage.googleapis.com/{bucket}/{name}`
 * and `gs://{bucket}/{name}`; with a [publicBaseUrl], `{publicBaseUrl}/{name}` as well, which is then
 * what new files are handed out as.
 */
class GcsAddresses(private val bucket: String, publicBaseUrl: String? = null) : BucketAddresses {
    private val publicBase = publicBaseUrl?.let(::UnderBase)

    override fun nameOf(address: String): String? {
        if (address.any(Char::isWhitespace)) return null
        return (publicBase?.nameOf(address) ?: blobNameIn(bucket, address))?.takeIf(BucketAddresses::isPlainName)
    }

    override fun addressOf(name: String): String = publicBase?.addressOf(name) ?: mediaLinkOf(name)

    override fun mentionedIn(text: String): Boolean =
        text.contains("gs://$bucket/") ||
            text.contains("storage.googleapis.com/$bucket/") ||
            text.contains("storage.googleapis.com/download/storage/v1/b/$bucket/o/") ||
            publicBase?.mentionedIn(text) == true

    override fun formsOf(name: String): List<String> = listOfNotNull(
        publicBase?.addressOf(name),
        mediaLinkOf(name),
        "https://storage.googleapis.com/$bucket/$name",
        "gs://$bucket/$name"
    )

    /** The media link an upload answers, less its generation, so it always opens the current file. */
    private fun mediaLinkOf(name: String) =
        "https://storage.googleapis.com/download/storage/v1/b/$bucket/o/${
            URLEncoder.encode(name, Charsets.UTF_8).replace("+", "%20")
        }?alt=media"

    private fun blobNameIn(bucket: String, url: String): String? {
        // Example: https://storage.googleapis.com/download/storage/v1/b/bucket-name/o/blob-name?generation=123&alt=media
        MEDIA_LINK.matchEntire(url)?.let { match ->
            if (match.groupValues[1] != bucket) return null
            return try {
                URLDecoder.decode(match.groupValues[2], Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        // Example: gs://bucket-name/blob-name or https://storage.googleapis.com/bucket-name/blob-name
        DIRECT.matchEntire(url)?.let { match ->
            if (match.groupValues[1] != bucket) return null
            return match.groupValues[2]
        }

        return null
    }

    private companion object {
        val MEDIA_LINK = Regex("https://storage\\.googleapis\\.com/download/storage/v1/b/([^/]+)/o/([^?]+)(?:\\?.*)?")
        val DIRECT = Regex("(?:gs://|https://storage\\.googleapis\\.com/)([^/]+)/(.+)")
    }
}
