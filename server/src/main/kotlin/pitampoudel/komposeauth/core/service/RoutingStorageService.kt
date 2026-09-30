package pitampoudel.komposeauth.core.service

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.util.concurrent.atomic.AtomicReference

/**
 * The [StorageService] everything is handed: new files go to the store `storageProvider` names (or
 * the only bucket configured; neither cloud is a default, see [AppConfig.resolvedStorageProvider]),
 * and a stored address is deleted from whichever store it names.
 *
 * Files are never copied between the two. One written to either store before a switch stays there
 * and is still read from its own address, so both stores' settings stay while any files are left.
 */
@Primary
@Service
class RoutingStorageService(
    private val appConfigService: AppConfigService,
    private val gcs: GcpStorageService
) : StorageService {
    private val log = LoggerFactory.getLogger(javaClass)

    private val s3 = AtomicReference<S3StorageService?>()

    /** The S3 store the config names now, rebuilt when an admin changes it. */
    private fun s3OrNull(): S3StorageService? {
        val config = appConfigService.getConfig()
        val bucket = config.s3BucketName ?: return null
        val settings = S3StorageService.Settings(
            bucket = bucket,
            region = config.s3Region ?: error("s3Region must be set with s3BucketName"),
            accessKeyId = config.s3AccessKeyId,
            secretAccessKey = config.s3SecretAccessKey
        )
        return s3.updateAndGet { current ->
            if (current != null && current.settings == settings) current else S3StorageService(settings)
        }
    }

    private fun writeStore(): StorageService = when (appConfigService.getConfig().resolvedStorageProvider()) {
        AppConfig.STORAGE_S3 -> s3OrNull() ?: error("unreachable: the resolved store is configured")
        else -> gcs
    }

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String =
        writeStore().upload(blobName, contentType, bytes)

    override fun download(blobName: String): ByteArray? = writeStore().download(blobName)

    override fun exists(blobName: String): Boolean = writeStore().exists(blobName)

    override fun delete(url: String): Boolean {
        s3OrNull()?.takeIf { it.owns(url) }?.let { return it.delete(url) }
        if (appConfigService.getConfig().gcpBucketName == null) return false
        // Best effort: a file left behind costs a few bytes, and failing here would stop the photo
        // or logo that replaces it from being saved.
        return runCatching { gcs.delete(url) }
            .onFailure { log.warn("Could not delete an old file from GCS", it) }
            .getOrDefault(false)
    }
}
