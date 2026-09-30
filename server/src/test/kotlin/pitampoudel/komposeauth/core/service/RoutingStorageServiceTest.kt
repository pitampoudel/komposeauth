package pitampoudel.komposeauth.core.service

import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
    private val gcsUrl = "https://storage.googleapis.com/download/storage/v1/b/old/o/users%2Fu1%2Fphoto?alt=media"

    private fun router(config: AppConfig): RoutingStorageService {
        val appConfigService: AppConfigService = mock { on { getConfig() } doReturn config }
        return RoutingStorageService(appConfigService)
    }

    /** Runs [block] with every GCS store the router builds replaced by a mock that answers [gcsUrl]. */
    private fun <T> withMockGcs(block: (built: () -> List<GcpStorageService>) -> T): T =
        mockConstruction(GcpStorageService::class.java) { gcs, _ ->
            whenever(gcs.upload(any(), anyOrNull(), any())) doReturn gcsUrl
            whenever(gcs.delete(any())) doReturn true
        }.use { construction -> block { construction.constructed() } }

    private val onS3 = AppConfig(
        storageProvider = AppConfig.STORAGE_S3,
        gcpProjectId = "p",
        gcpBucketName = "old",
        s3BucketName = "new-files",
        s3Region = "ap-south-1"
    )

    @Test
    fun `with only GCS configured new files go to GCS`() = withMockGcs { built ->
        assertEquals(gcsUrl, router(AppConfig(gcpProjectId = "p", gcpBucketName = "old")).upload("a", null, ByteArray(1)))
        assertEquals(1, built().size)
    }

    @Test
    fun `with both buckets storageProvider decides, and GCS can be chosen`() = withMockGcs {
        assertEquals(gcsUrl, router(onS3.copy(storageProvider = AppConfig.STORAGE_GCS)).upload("a", null, ByteArray(1)))
    }

    @Test
    fun `with both buckets and no choice neither cloud is picked`() = withMockGcs { built ->
        assertFailsWith<IllegalStateException> { router(onS3.copy(storageProvider = null)).upload("a", null, ByteArray(1)) }
        assertTrue(built().isEmpty())
    }

    @Test
    fun `the only bucket configured is used, and a choice with no bucket is refused`() {
        assertEquals("s3", onS3.copy(storageProvider = null, gcpBucketName = null).resolvedStorageProvider())
        assertFailsWith<IllegalStateException> {
            AppConfig(gcpBucketName = "old", storageProvider = AppConfig.STORAGE_S3).resolvedStorageProvider()
        }
    }

    @Test
    fun `the GCS store is built once and reused`() = withMockGcs { built ->
        val router = router(AppConfig(gcpProjectId = "p", gcpBucketName = "old"))
        router.upload("a", null, ByteArray(1))
        router.upload("b", null, ByteArray(1))
        assertEquals(1, built().size)
    }

    @Test
    fun `a delete goes to the store new files go to`() = withMockGcs { built ->
        assertTrue(router(onS3.copy(storageProvider = AppConfig.STORAGE_GCS)).delete(gcsUrl))
        verify(built().single()).delete(gcsUrl)

        assertFalse(router(onS3).delete(gcsUrl), "the S3 store does not delete an address that is not its own")
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
