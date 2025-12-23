package pitampoudel.komposeauth.kyc

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.repository.AppConfigRepository
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.jwk.service.JwkService
import java.security.KeyPairGenerator
import java.util.Optional
import java.util.UUID

@TestConfiguration
class KycFlowTestOverrides {

    @Bean
    @Primary
    fun storageService(): StorageService = object : StorageService {
        override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String = "test://$blobName"
        override fun download(blobName: String): ByteArray? = null
        override fun exists(blobName: String): Boolean = false
        override fun delete(url: String): Boolean = true
    }

    @Bean
    @Primary
    fun emailService(): EmailService {
        // Deterministic no-op.
        return mock {
            on { sendHtmlMail(any(), any(), any(), any(), anyOrNull()) } doReturn true
        }
    }

    @Bean
    @Primary
    fun jwkService(): JwkService {
        val keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair()
        return mock {
            on { loadOrCreateKeyPair() } doReturn keyPair
            on { currentKid() } doReturn UUID.randomUUID().toString()
        }
    }

    @Bean
    @Primary
    fun appConfigProvider(): AppConfigProvider {
        val fakeRepo: AppConfigRepository = mock {
            // Avoid hitting Mongo for config.
            on { findById(any()) } doReturn Optional.of(AppConfig())
        }
        val fakeCrypto: CryptoService = mock {
            on { encrypt(any()) } doReturn ""
            on { decrypt(any()) } doReturn ""
        }

        return object : AppConfigProvider(repo = fakeRepo, crypto = fakeCrypto) {
            override fun get(): AppConfig {
                return AppConfig(
                    name = "Test",
                    smtpHost = "localhost",
                    smtpPort = 2525,
                )
            }
        }
    }
}
