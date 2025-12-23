package pitampoudel.komposeauth.core.service

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.Test
import kotlin.test.assertFalse

class GcpStorageServiceTest {

    @Test
    fun `delete returns false for unknown url`() {
        val appConfigService: AppConfigService = mock {
            on { getConfig() } doReturn AppConfig(gcpProjectId = "p", gcpBucketName = "b")
        }
        val sut = GcpStorageService(appConfigService)

        // Ensure we short-circuit before initializing the GCP client.
        assertFalse(sut.delete("https://example.com/not-gcp"))
    }

    @Test
    fun `delete returns false for empty url`() {
        val appConfigService: AppConfigService = mock {
            on { getConfig() } doReturn AppConfig(gcpProjectId = "p", gcpBucketName = "b")
        }
        val sut = GcpStorageService(appConfigService)

        // Ensure we short-circuit before initializing the GCP client.
        assertFalse(sut.delete(""))
    }
}
