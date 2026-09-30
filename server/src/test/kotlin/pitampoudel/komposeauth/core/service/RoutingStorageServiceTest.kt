package pitampoudel.komposeauth.core.service

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutingStorageServiceTest {
    private val gcs: GcpStorageService = mock()
    private val gcsUrl = "https://storage.googleapis.com/download/storage/v1/b/old/o/users%2Fu1%2Fphoto?alt=media"

    private fun router(config: AppConfig): RoutingStorageService {
        val appConfigService: AppConfigService = mock { on { getConfig() } doReturn config }
        return RoutingStorageService(appConfigService)
    }

    private val onS3 = AppConfig(
        storageProvider = AppConfig.STORAGE_S3,
        gcpProjectId = "p",
        gcpBucketName = "old",
        s3BucketName = "new-files",
        s3Region = "ap-south-1"
    )

    @Test
    fun `with only GCS configured new files go to GCS`() {
        whenever(gcs.upload(any(), anyOrNull(), any())) doReturn gcsUrl

        assertEquals(gcsUrl, router(AppConfig(gcpProjectId = "p", gcpBucketName = "old")).upload("a", null, ByteArray(1)))
    }

    @Test
    fun `with both buckets storageProvider decides, and GCS can be chosen`() {
        whenever(gcs.upload(any(), anyOrNull(), any())) doReturn gcsUrl

        assertEquals(gcsUrl, router(onS3.copy(storageProvider = AppConfig.STORAGE_GCS)).upload("a", null, ByteArray(1)))
    }

    @Test
    fun `with both buckets and no choice neither cloud is picked`() {
        assertFailsWith<IllegalStateException> { router(onS3.copy(storageProvider = null)).upload("a", null, ByteArray(1)) }
        verify(gcs, never()).upload(any(), anyOrNull(), any())
    }

    @Test
    fun `the only bucket configured is used, and a choice with no bucket is refused`() {
        assertEquals("s3", onS3.copy(storageProvider = null, gcpBucketName = null).resolvedStorageProvider())
        assertFailsWith<IllegalStateException> {
            AppConfig(gcpBucketName = "old", storageProvider = AppConfig.STORAGE_S3).resolvedStorageProvider()
        }
    }

    @Test
    fun `a file written before the switch is deleted from GCS`() {
        whenever(gcs.delete(gcsUrl)) doReturn true

        assertTrue(router(onS3).delete(gcsUrl))
        verify(gcs).delete(gcsUrl)
    }

    @Test
    fun `a GCS that no longer answers does not stop the replacement being saved`() {
        whenever(gcs.delete(gcsUrl)) doThrow IllegalStateException("billing disabled")

        assertFalse(router(onS3).delete(gcsUrl))
    }

    @Test
    fun `with no GCS configured an old address is left alone`() {
        assertFalse(router(onS3.copy(gcpProjectId = null, gcpBucketName = null)).delete(gcsUrl))
        verify(gcs, never()).delete(any())
    }

    @Test
    fun `the S3 store recognises only its own addresses, version and all`() {
        val s3 = S3StorageService(S3StorageService.Settings(bucket = "new-files", region = "ap-south-1"))

        assertTrue(s3.owns("https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo?v=1700000000000"))
        assertFalse(s3.owns("https://other.s3.ap-south-1.amazonaws.com/users/u1/photo"))
        assertFalse(s3.owns(gcsUrl))
        assertFalse(s3.delete(gcsUrl))
    }
}
