package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.NoopEmailTestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ErrorCode
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.otp.repository.OtpRepository
import pitampoudel.komposeauth.otp.service.OtpCodes
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.user.domain.OtpType
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
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

    @Autowired
    private lateinit var otpCodes: OtpCodes


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
            content = json.encodeToString(
                VerifyOtpRequest.serializer(),
                VerifyOtpRequest(username = email, otp = "000000", type = OtpType.EMAIL)
            )
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
     * `verifyEmail` used to mark whatever address the account held at click time, so a link still
     * outstanding when the address changed would vouch for the new one, or put the old one back.
     */
    @Test
    fun `a link cannot verify an address it was not sent to`() {
        val original = "binding-original@example.com"
        val swapped = "binding-swapped@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, original)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, original)

        // The link goes out to the address the account holds now.
        val token = verificationToken(userId, original)

        // Then the address is changed, the only way it can be: by proving the new one.
        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(SendOtpRequest.serializer(), SendOtpRequest(swapped, type = OtpType.EMAIL))
        }.andExpect { status { isOk() } }
        val otp = otpRepository.findByReceiverOrderByCreatedAtDesc(swapped).first().otp
        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                VerifyOtpRequest.serializer(),
                VerifyOtpRequest(username = swapped, otp = otp, type = OtpType.EMAIL)
            )
        }.andExpect { status { isOk() } }
        assertEquals(swapped, userRepository.findById(ObjectId(userId)).orElseThrow().email)

        // The old link is now stale. It proves the original address, which the account no longer
        // holds, and it must not put it back.
        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
        }.andExpect {
            status { is4xxClientError() }
        }

        assertEquals(
            swapped,
            userRepository.findById(ObjectId(userId)).orElseThrow().email,
            "the stale link must not revert the address"
        )
    }

    /**
     * An address taken on trust would let its real owner, signing in later through Google or a code,
     * land in this account instead of their own.
     */
    @Test
    fun `a profile update cannot change the email address`() {
        val email = "profile-email-locked@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                UpdateProfileRequest.serializer(),
                UpdateProfileRequest(email = "someone-else@example.com", currentPassword = "Password1")
            )
        }.andExpect { status { isBadRequest() } }

        assertEquals(email, userRepository.findById(ObjectId(userId)).orElseThrow().email)
    }

    private fun otpFor(email: String, signIn: MockHttpServletRequestDsl.() -> Unit): String {
        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            signIn()
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(SendOtpRequest.serializer(), SendOtpRequest(email, type = OtpType.EMAIL))
        }.andExpect { status { isOk() } }
        return otpRepository.findByReceiverOrderByCreatedAtDesc(email).first().otp
    }

    private fun verifyOtp(
        email: String,
        otp: String,
        currentPassword: String? = null,
        signIn: MockHttpServletRequestDsl.() -> Unit
    ): MockHttpServletResponse = mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
        signIn()
        contentType = MediaType.APPLICATION_JSON
        content = json.encodeToString(
            VerifyOtpRequest.serializer(),
            VerifyOtpRequest(username = email, otp = otp, type = OtpType.EMAIL, currentPassword = currentPassword)
        )
    }.andReturn().response

    private fun codeOf(response: MockHttpServletResponse): String? =
        json.parseToJsonElement(response.contentAsString).jsonObject[ErrorCode.PROPERTY]?.jsonPrimitive?.content

    private fun tokenLogin(credential: Credential): Pair<String, String> {
        val response = mockMvc.post("/${ApiEndpoints.LOGIN}") {
            param("responseType", ResponseType.TOKEN.name)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString<Credential>(credential)
        }.andExpect { status { isOk() } }.andReturn().response
        val body = json.parseToJsonElement(response.contentAsString).jsonObject
        return body.getValue("access_token").jsonPrimitive.content to body.getValue("refresh_token").jsonPrimitive.content
    }

    @Test
    fun `replacing a verified email takes the current password, and the refusal says so in a code`() {
        val email = "replace-verified@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)
        val signedIn: MockHttpServletRequestDsl.() -> Unit = { cookie(cookie) }
        // Proving the address the account was created with is setting it, not replacing it.
        assertEquals(200, verifyOtp(email, otpFor(email, signedIn), signIn = signedIn).status)

        val replacement = "replace-verified-new@example.com"
        val otp = otpFor(replacement, signedIn)
        // Freshly signed in, but the account has a password, and the password is the proof.
        val refused = verifyOtp(replacement, otp, signIn = signedIn)
        assertEquals(403, refused.status, refused.contentAsString)
        assertEquals(ErrorCode.CURRENT_PASSWORD_REQUIRED.name, codeOf(refused))
        assertEquals(403, verifyOtp(replacement, otp, currentPassword = "wrong", signIn = signedIn).status)

        val replaced = verifyOtp(replacement, otp, currentPassword = "Password1", signIn = signedIn)
        assertEquals(200, replaced.status, replaced.contentAsString)
        assertEquals(replacement, userRepository.findById(ObjectId(userId)).orElseThrow().email)
    }

    @Test
    fun `an account without a password replaces its verified email only with a fresh sign-in`() {
        val email = "replace-passwordless@example.com"
        val userId = ObjectId(TestAuthHelpers.createUser(mockMvc, json, email))
        val (signInToken, refreshToken) = tokenLogin(Credential.UsernamePassword(username = email, password = "Password1"))
        val (refreshedToken, _) = tokenLogin(Credential.RefreshToken(refreshToken))
        userRepository.save(userRepository.findById(userId).orElseThrow().copy(passwordHash = null, emailVerified = true))
        val fresh: MockHttpServletRequestDsl.() -> Unit = { header("Authorization", "Bearer $signInToken") }
        val refreshed: MockHttpServletRequestDsl.() -> Unit = { header("Authorization", "Bearer $refreshedToken") }

        val replacement = "replace-passwordless-new@example.com"
        val otp = otpFor(replacement, fresh)
        // A refreshed token says nothing about when the person last signed in.
        val refused = verifyOtp(replacement, otp, signIn = refreshed)
        assertEquals(403, refused.status, refused.contentAsString)
        assertEquals(ErrorCode.RECENT_SIGN_IN_REQUIRED.name, codeOf(refused))

        assertEquals(200, verifyOtp(replacement, otp, signIn = fresh).status)
        assertEquals(replacement, userRepository.findById(userId).orElseThrow().email)
    }

    @Test
    fun `a code for an address that belongs to someone else is refused`() {
        val owner = "otp-owner-a@example.com"
        val other = "otp-owner-b@example.com"
        TestAuthHelpers.createUser(mockMvc, json, owner)
        TestAuthHelpers.createUser(mockMvc, json, other)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, owner)

        val refused = verifyOtp(other, otpCodes.issue(other), currentPassword = "Password1") { cookie(cookie) }

        assertEquals(403, refused.status, refused.contentAsString)
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
