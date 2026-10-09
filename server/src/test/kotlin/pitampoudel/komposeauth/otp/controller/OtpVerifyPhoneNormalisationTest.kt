package pitampoudel.komposeauth.otp.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.security.ratelimit.RateLimitProperties
import pitampoudel.komposeauth.core.security.ratelimit.RateLimiter
import pitampoudel.komposeauth.core.service.email.EmailVerificationService
import pitampoudel.komposeauth.core.utils.ServerUrl
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.user.domain.OtpType
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The code is filed under the number it was sent to, in E.164, so both legs parse to that form. A
 * number typed without a leading `+` (as a sign-up screen asking for "98XXXXXXXX" gets it) is read in
 * the configured default region; with no region at all libphonenumber refused it on both legs. A
 * plain unit test rather than an integration one, so it runs without Docker.
 */
class OtpVerifyPhoneNormalisationTest {

    private val userService = mockk<UserService>()
    private val userContextService = mockk<UserContextService>()
    private val emailVerificationService = mockk<EmailVerificationService>()
    private val phoneNumberVerificationService = mockk<PhoneNumberVerificationService>()
    private val rateLimiter = mockk<RateLimiter>(relaxed = true)
    private val appConfigService = mockk<AppConfigService> { every { defaultPhoneRegion() } returns "NP" }

    private val controller = OtpVerifyController(
        userService = userService,
        userContextService = userContextService,
        emailVerificationService = emailVerificationService,
        phoneNumberVerificationService = phoneNumberVerificationService,
        rateLimiter = rateLimiter,
        // Off, so the target quota never touches the rate limiter in a plain unit test.
        rateLimitProperties = RateLimitProperties().apply { enabled = false },
        serverUrl = ServerUrl(AuthorizationServerSettings.builder().build()),
        appConfigService = appConfigService
    )

    private val caller = User(
        id = ObjectId.get(),
        firstName = "Owner",
        lastName = "One",
        email = "owner@example.com",
        phoneNumber = null
    )

    private fun verifying(username: String): String {
        every { userContextService.getUserFromAuthentication() } returns caller
        every { userService.findByUserName(any()) } returns null
        val phone = slot<String>()
        every { userService.verifyPhoneNumber(caller.id, capture(phone), "123456") } returns mockk<UserResponse>()

        controller.verifyOTP(VerifyOtpRequest(username = username, type = OtpType.PHONE, otp = "123456"))

        verify(exactly = 1) { userService.verifyPhoneNumber(caller.id, any(), "123456") }
        return phone.captured
    }

    private fun sending(username: String): String {
        every { userContextService.authenticatedUserOrNull() } returns caller
        every { userService.findByUserName(any()) } returns null
        val phone = slot<String>()
        every { phoneNumberVerificationService.initiate(capture(phone)) } returns MessageResponse("sent")

        controller.sendOtp(SendOtpRequest(username = username, type = OtpType.PHONE), mockk(relaxed = true))

        return phone.captured
    }

    @Test
    fun `a national number is sent to in E164`() {
        assertEquals("+9779812345678", sending("9812345678"))
    }

    @Test
    fun `a national number is verified in E164`() {
        assertEquals("+9779812345678", verifying("9812345678"))
    }

    @Test
    fun `a national number is read in the configured region`() {
        every { appConfigService.defaultPhoneRegion() } returns "IN"

        assertEquals("+919812345678", sending("9812345678"))
        assertEquals("+919812345678", verifying("9812345678"))
    }

    @Test
    fun `a number with a country code ignores the default region`() {
        every { appConfigService.defaultPhoneRegion() } returns "IN"

        assertEquals("+9779812345678", sending("+9779812345678"))
    }

    @Test
    fun `a number given in E164 is verified as it stands`() {
        assertEquals("+9779812345678", verifying("+9779812345678"))
    }

    @Test
    fun `a number given with spaces is verified in E164`() {
        assertEquals("+9779812345678", verifying("+977 98 1234 5678"))
    }

    @Test
    fun `an unparseable number is refused rather than stored`() {
        every { userContextService.getUserFromAuthentication() } returns caller

        assertFailsWith<IllegalArgumentException> {
            controller.verifyOTP(VerifyOtpRequest(username = "not-a-phone", type = OtpType.PHONE, otp = "123456"))
        }
        verify(exactly = 0) { userService.verifyPhoneNumber(any(), any(), any()) }
    }
}
