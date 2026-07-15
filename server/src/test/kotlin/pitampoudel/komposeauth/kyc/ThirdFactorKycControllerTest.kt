package pitampoudel.komposeauth.kyc

import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.service.jwt.JwtTokenService
import pitampoudel.komposeauth.kyc.controller.ThirdFactorKycController
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService

/**
 * POST /third-factor-kyc is public — a valid signature only proves we issued the token, so the
 * identifier in the body must be bound to the identifier in the token.
 */
class ThirdFactorKycControllerTest {

    private val secret = "a-test-secret-that-is-at-least-32-bytes-long"
    private val jwtTokenService = JwtTokenService()
    private val kycService = mock<KycService>()
    private val appConfigService = mock<AppConfigService>()

    private val controller = ThirdFactorKycController(
        userService = mock<UserService>(),
        kycService = kycService,
        appConfigService = appConfigService,
        jwtTokenService = jwtTokenService,
        userContextService = mock<UserContextService>(),
        kycRepo = mock<KycVerificationRepository>(),
        restClient = mock()
    )

    private fun tokenFor(identifier: String) = jwtTokenService.generateHs256Token(
        secretKey = secret,
        subject = identifier,
        issuer = "https://example.test",
        claims = mapOf("identifier" to identifier)
    )

    private fun payload(jwt: String, identifier: String) = ThirdFactorModel(identifier = identifier, jwt = jwt)

    private fun setUp() {
        whenever(appConfigService.getConfig())
            .thenReturn(AppConfig().apply { thirdFactorSecretKey = secret })
    }

    @Test
    fun `rejects a validly signed token whose identifier names a different user`() {
        setUp()
        val attacker = ObjectId().toHexString()
        val victim = ObjectId().toHexString()

        // The attacker holds a genuine token from their own KYC session.
        val data = payload(jwt = tokenFor(attacker), identifier = victim)

        assertThrows<BadRequestException> {
            controller.submit(MockHttpServletRequest(), data)
        }
        verify(kycService, never()).submitThirdFactorVerification(any(), any())
    }

    @Test
    fun `accepts a token whose identifier matches the body`() {
        setUp()
        val userId = ObjectId().toHexString()
        val data = payload(jwt = tokenFor(userId), identifier = userId)

        controller.submit(MockHttpServletRequest(), data)

        verify(kycService).submitThirdFactorVerification(any(), any())
    }
}
