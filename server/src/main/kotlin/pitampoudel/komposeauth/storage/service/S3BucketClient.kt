package pitampoudel.komposeauth.storage.service

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI
import java.time.Clock

/**
 * [BucketClient] over a bucket on any service speaking the S3 API: AWS itself, or, through an
 * [Settings.endpoint], Cloudflare R2, DigitalOcean Spaces, Backblaze B2, Wasabi, Hetzner or MinIO. Not
 * a bean of its own: each bucket on the Storage page gets one, built by
 * [pitampoudel.komposeauth.storage.service.CloudBucketClients].
 *
 * Which objects anyone can read is the bucket policy's business, as it is on GCS.
 */
class S3BucketClient(
    val settings: Settings,
    private val clock: Clock = Clock.systemUTC()
) : BucketClient {

    data class Settings(
        val bucket: String,
        val region: String,
        /** A service other than AWS, addressed path-style. */
        val endpoint: String? = null,
        val publicBaseUrl: String? = null,
        /** Both null: the host's own credentials (an ECS task role, an instance profile, the environment). */
        val accessKeyId: String? = null,
        val secretAccessKey: String? = null
    ) {
        override fun toString() =
            "Settings(bucket=$bucket, region=$region, endpoint=$endpoint, publicBaseUrl=$publicBaseUrl, accessKeyId=$accessKeyId)"
    }

    val addresses = S3Addresses(settings.bucket, settings.region, settings.endpoint, settings.publicBaseUrl)

    private val client: S3Client by lazy {
        S3Client.builder()
            .region(Region.of(settings.region))
            .credentialsProvider(
                if (settings.accessKeyId != null && settings.secretAccessKey != null)
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(settings.accessKeyId, settings.secretAccessKey)
                    )
                else DefaultCredentialsProvider.builder().build()
            )
            .apply {
                settings.endpoint?.let { endpoint ->
                    endpointOverride(URI.create(endpoint))
                    forcePathStyle(true)
                    // The SDK's default checksums are sent as trailers several S3-compatible
                    // services refuse; AWS itself keeps them.
                    requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                }
            }
            .build()
    }

    /**
     * The object's address, with a version on the end. A profile photo is always written to the same
     * name, and GCS's links changed with every write; without the version a browser would keep
     * showing the old photo.
     */
    override fun upload(name: String, contentType: String?, bytes: ByteArray): String {
        client.putObject(
            PutObjectRequest.builder()
                .bucket(settings.bucket)
                .key(name)
                .apply { contentType?.let { contentType(it) } }
                .build(),
            RequestBody.fromBytes(bytes)
        )
        return "${addresses.addressOf(name)}?v=${clock.millis()}"
    }

    override fun download(name: String): ByteArray? =
        try {
            client.getObjectAsBytes(GetObjectRequest.builder().bucket(settings.bucket).key(name).build()).asByteArray()
        } catch (_: NoSuchKeyException) {
            null
        }

    override fun delete(name: String): Boolean {
        client.deleteObject(DeleteObjectRequest.builder().bucket(settings.bucket).key(name).build())
        return true
    }

    override fun stat(name: String): StoredObject? =
        try {
            val head = client.headObject(HeadObjectRequest.builder().bucket(settings.bucket).key(name).build())
            StoredObject(name, head.contentLength() ?: 0L, head.lastModified())
        } catch (ex: S3Exception) {
            // A HEAD has no body to name the error by, so a missing key is only a 404.
            if (ex.statusCode() == 404) null else throw ex
        }
}
