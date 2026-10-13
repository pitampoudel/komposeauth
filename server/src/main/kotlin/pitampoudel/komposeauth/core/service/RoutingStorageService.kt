package pitampoudel.komposeauth.core.service

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.util.concurrent.atomic.AtomicReference

/**
 * The [StorageService] everything is handed: new files go to the store `storageProvider` names (or
 * the only bucket configured; neither cloud is a default, see [AppConfig.resolvedStorageProvider]),
 * and a read or a delete goes to whichever store the address is in.
 *
 * The app never copies files between the two. One written to either store before a switch stays
 * there and is still read from its own address, so both stores' settings stay while any files are
 * left — until the GCS bucket is copied into S3 and `gcsFilesReadFrom` says so, when its addresses
 * are answered from S3 ([GcsAddressesOnS3]) and Google's credentials can go.
 */
@Primary
@Service
class RoutingStorageService(
    private val appConfigService: AppConfigService,
) : StorageService {

    private val s3 = AtomicReference<S3StorageService?>()
    private val gcs = AtomicReference<GcpStorageService?>()

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

    /** The GCS store the config names now, rebuilt when an admin changes it. */
    private fun gcsOrNull(): GcpStorageService? {
        val config = appConfigService.getConfig()
        val bucket = config.gcpBucketName ?: return null
        val settings = GcpStorageService.Settings(projectId = config.gcpProjectId, bucket = bucket)
        return gcs.updateAndGet { current ->
            if (current != null && current.settings == settings) current else GcpStorageService(settings)
        }
    }

    private fun writeStore(): StorageService = when (appConfigService.getConfig().resolvedStorageProvider()) {
        AppConfig.STORAGE_S3 -> s3OrNull() ?: error("unreachable: the resolved store is configured")
        AppConfig.STORAGE_GCS -> gcsOrNull() ?: error("unreachable: the resolved store is configured")
        else -> error("unreachable: the resolved store is configured")
    }

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String =
        writeStore().upload(blobName, contentType, bytes)

    override fun delete(url: String): Boolean {
        val store = storeHolding(url) ?: return false
        return store.delete(url)
    }

    override fun download(url: String): ByteArray? {
        val store = storeHolding(url) ?: error("No configured store holds $url")
        return store.download(url)
    }

    private fun storeHolding(url: String): StorageService? {
        val s3 = s3OrNull()
        s3?.takeIf { it.owns(url) }?.let { return it }
        val config = appConfigService.getConfig()
        if (config.gcsFilesAreOnS3()) {
            val copied = GcsAddressesOnS3(
                gcsBucket = config.gcpBucketName ?: error("unreachable: gcsFilesAreOnS3 checks the GCS bucket"),
                copy = s3 ?: error("unreachable: gcsFilesAreOnS3 checks the S3 bucket")
            )
            return copied.takeIf { it.owns(url) }
        }
        return gcsOrNull()?.takeIf { it.owns(url) }
    }
}
