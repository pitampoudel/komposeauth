package pitampoudel.komposeauth.storage.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import pitampoudel.komposeauth.storage.service.BucketAddresses
import pitampoudel.komposeauth.storage.service.GcsAddresses
import pitampoudel.komposeauth.storage.service.S3Addresses
import java.time.Instant

/**
 * A bucket files are kept in, as listed on the admin console's Storage page.
 *
 * At most one bucket is [primary] and takes new files. Every other one still answers the addresses
 * already on record, since an address names the bucket it was written to — and a bucket whose files
 * an admin copied elsewhere answers them from there ([readFromBucketId]).
 *
 * [kind], [bucketName], [region], [endpoint] and [publicBaseUrl] decide what this bucket's addresses
 * look like, so they are fixed once it is created: changing one would orphan every address already saved.
 */
@Document("storage_buckets")
@TypeAlias("storage_bucket")
// At most one primary, whatever two admins or two instances do at once.
@CompoundIndex(name = "one_primary", def = "{'primary': 1}", unique = true, partialFilter = "{'primary': true}")
data class StorageBucket(
    @Id
    val id: ObjectId = ObjectId(),
    val name: String,
    val kind: Kind,
    val bucketName: String,
    /** S3 only. Cloudflare R2 uses `auto`. */
    val region: String? = null,
    /** S3 only: a service other than AWS, addressed path-style as `{endpoint}/{bucketName}/{name}`. */
    val endpoint: String? = null,
    /** A CDN or public domain in front of the bucket; new files are handed out under it. */
    val publicBaseUrl: String? = null,
    /** GCS only. */
    val projectId: String? = null,
    /** S3: with [secretAccessKey]. No credential at all signs in as the host's own identity. */
    val accessKeyId: String? = null,
    /** Encrypted at rest, as the secrets in the app config are. */
    val secretAccessKey: String? = null,
    /** GCS: a service account key. Encrypted at rest. */
    val serviceAccountJson: String? = null,
    val primary: Boolean = false,
    /** An admin copied its files to that bucket, and they are read from there under the same names. */
    val readFromBucketId: ObjectId? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    enum class Kind(val label: String) {
        GCS("Google Cloud Storage"),
        /** Anything speaking the S3 API, AWS or not. */
        S3("S3-compatible")
    }

    fun addresses(): BucketAddresses = when (kind) {
        Kind.GCS -> GcsAddresses(bucketName, publicBaseUrl)
        Kind.S3 -> S3Addresses(bucketName, region ?: error("An S3 bucket needs a region"), endpoint, publicBaseUrl)
    }

    fun usesHostIdentity(): Boolean = accessKeyId == null && secretAccessKey == null && serviceAccountJson == null

    override fun toString() =
        "StorageBucket(id=$id, name=$name, kind=$kind, bucketName=$bucketName, region=$region, endpoint=$endpoint, " +
            "publicBaseUrl=$publicBaseUrl, primary=$primary, readFromBucketId=$readFromBucketId)"
}
