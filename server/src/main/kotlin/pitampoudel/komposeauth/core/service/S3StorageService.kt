package pitampoudel.komposeauth.core.service

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
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
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Clock

/**
 * [StorageService] over an S3 bucket. Not a bean of its own: [RoutingStorageService] builds one
 * for whatever bucket the config names.
 *
 * Which objects anyone can read is the bucket policy's business, as it is on GCS.
 */
class S3StorageService(
    val settings: Settings,
    private val clock: Clock = Clock.systemUTC()
) : StorageService {

    data class Settings(
        val bucket: String,
        val region: String,
        /** Both null: the host's own credentials (an ECS task role, an instance profile, the environment). */
        val accessKeyId: String? = null,
        val secretAccessKey: String? = null
    )

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
            .build()
    }

    private val baseUrl = "https://${settings.bucket}.s3.${settings.region}.amazonaws.com/"

    /**
     * The object's address, with a version on the end. A profile photo is always written to the same
     * name, and GCS's links changed with every write; without the version a browser would keep
     * showing the old photo.
     */
    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        client.putObject(
            PutObjectRequest.builder()
                .bucket(settings.bucket)
                .key(blobName)
                .apply { contentType?.let { contentType(it) } }
                .build(),
            RequestBody.fromBytes(bytes)
        )
        return "$baseUrl${encodePath(blobName)}?v=${clock.millis()}"
    }

    override fun download(blobName: String): ByteArray? = try {
        client.getObjectAsBytes(GetObjectRequest.builder().bucket(settings.bucket).key(blobName).build())
            .asByteArray()
    } catch (_: NoSuchKeyException) {
        null
    }

    override fun exists(blobName: String): Boolean = try {
        client.headObject(HeadObjectRequest.builder().bucket(settings.bucket).key(blobName).build())
        true
    } catch (ex: S3Exception) {
        if (ex.statusCode() == 404) false else throw ex
    }

    /** True when [url] is an address this bucket handed out. */
    fun owns(url: String): Boolean = keyOf(url) != null

    override fun delete(url: String): Boolean {
        val key = keyOf(url) ?: return false
        client.deleteObject(DeleteObjectRequest.builder().bucket(settings.bucket).key(key).build())
        return true
    }

    private fun keyOf(url: String): String? {
        if (!url.startsWith(baseUrl)) return null
        val path = URI(url).rawPath.removePrefix("/")
        if (path.isEmpty()) return null
        return path.split('/').joinToString("/") { URLDecoder.decode(it, Charsets.UTF_8) }
    }

    private fun encodePath(key: String): String =
        key.split('/').joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8).replace("+", "%20") }
}
