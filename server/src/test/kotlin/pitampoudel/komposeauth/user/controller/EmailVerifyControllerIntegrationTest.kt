package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.NoopEmailTestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.otp.repository.OtpRepository
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.user.domain.OtpType
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class, NoopEmailTestConfig::class)
@AutoConfigureMockMvc
class EmailVerifyControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var oneTimeTokenService: OneTimeTokenService

    @Autowired
    private lateinit var otpRepository: OtpRepository


    @Test
    fun `sendEmailOtp succeeds and stores otp`() {
        val email = "otp-send@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(email, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }

        val otps = otpRepository.findByReceiverOrderByCreatedAtDesc(email)
        assert(otps.isNotEmpty())
    }

    @Test
    fun `sendEmailOtp forbids sending to another user`() {
        val ownerEmail = "owner@example.com"
        val otherEmail = "other@example.com"

        TestAuthHelpers.createUser(mockMvc, json, ownerEmail)
        TestAuthHelpers.createUser(mockMvc, json, otherEmail)
        val ownerCookie = TestAuthHelpers.loginCookie(mockMvc, json, ownerEmail)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(ownerCookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(otherEmail, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `verifyEmailOtp succeeds with valid otp`() {
        val email = "otp-verify@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(email, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }

        val otp = otpRepository.findByReceiverOrderByCreatedAtDesc(email).first().otp

        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                VerifyOtpRequest.serializer(),
                VerifyOtpRequest(username = "otp-verify@example.com", otp = otp, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `verifyEmailOtp fails with wrong otp`() {
        val email = "otp-verify-fail@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(email, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = "{\"otp\":\"000000\"}"
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `verifyEmail succeeds with valid token`() {
        val email = "verify-token@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)

        val token = verificationToken(userId, email)

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
        }.andExpect {
            status { is3xxRedirection() }
        }
    }

    /**
     * A link speaks for the address it was sent to, and for no other.
     *
     * `User.update` drops `emailVerified` when the address changes, which is right — nobody has
     * shown they can read mail at the new one. But `verifyEmail` used to mark whatever address the
     * account held at click time, so an outstanding link put the flag straight back, now attached to
     * an address its holder had never demonstrated reaching. Since `emailVerified` rides in the
     * access token and the OIDC `emailVerified` claim, a relying party keying accounts on a verified
     * address would have taken that at face value.
     */
    @Test
    fun `a link cannot verify an address it was not sent to`() {
        val original = "binding-original@example.com"
        val swapped = "binding-swapped@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, original)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, original)

        // The link goes out to the address the account holds now.
        val token = verificationToken(userId, original)

        // Then the address is changed. Verification is correctly dropped along with it.
        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                UpdateProfileRequest.serializer(),
                UpdateProfileRequest(email = swapped, currentPassword = "Password1")
            )
        }.andExpect { status { isOk() } }

        val afterSwap = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertEquals(swapped, afterSwap.email)
        assertFalse(afterSwap.emailVerified, "changing the address should drop verification")

        // The old link is now stale. It proves the original address, which the account no longer
        // holds, and it must not vouch for the new one.
        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
        }.andExpect {
            status { is4xxClientError() }
        }

        val afterClick = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertEquals(swapped, afterClick.email, "the stale link must not revert the address")
        assertFalse(
            afterClick.emailVerified,
            "an address nobody proved they can read was marked verified"
        )
    }

    /** A link issued the way the application issues one, with the address recorded on the token. */
    private fun verificationToken(userId: String, email: String): String =
        oneTimeTokenService.generateEmailVerificationLink(
            userId = ObjectId(userId),
            ttl = 1.minutes,
            baseUrl = "http://localhost",
            email = email
        ).substringAfter("token=")

    @Test
    fun `verifyEmail fails with invalid token`() {
        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", "invalid-token")
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
