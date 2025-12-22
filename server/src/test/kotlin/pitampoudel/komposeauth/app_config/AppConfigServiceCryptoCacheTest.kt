package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.repository.AppConfigRepository
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.service.security.CryptoService
import java.util.Optional
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class AppConfigServiceCryptoCacheTest {

    @Test
    fun `save encrypts secrets and get caches decrypted values`() {
        val repo = mock<AppConfigRepository>()
        val crypto = mock<CryptoService>()
        val configProvider = AppConfigProvider(repo, crypto)

        whenever(crypto.encrypt("secret")).thenReturn("enc(secret)")
        whenever(crypto.decrypt("enc(secret)")).thenReturn("secret")

        val toSave = AppConfig(
            googleAuthClientSecret = "secret",
        )

        whenever(repo.save(any())).thenAnswer { it.arguments[0] as AppConfig }

        val saved = configProvider.save(toSave)
        assertEquals("secret", saved.googleAuthClientSecret)

        // save() decrypts once when caching the saved entity.
        verify(crypto, times(1)).decrypt("enc(secret)")

        // Now simulate repo returning encrypted content.
        whenever(repo.findById(AppConfig.SINGLETON_ID)).thenReturn(
            Optional.of(
                AppConfig(
                    id = AppConfig.SINGLETON_ID,
                    googleAuthClientSecret = "enc(secret)"
                )
            )
        )

        configProvider.clearCache()
        val first = configProvider.get()
        val second = configProvider.get()

        assertEquals("secret", first.googleAuthClientSecret)
        assertEquals("secret", second.googleAuthClientSecret)

        // get() should decrypt once after cache clear; the second get() should be served from cache.
        verify(crypto, times(2)).decrypt("enc(secret)")
    }
}
