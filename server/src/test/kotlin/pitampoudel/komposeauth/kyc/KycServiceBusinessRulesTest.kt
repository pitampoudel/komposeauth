package pitampoudel.komposeauth.kyc

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.entity.User
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class KycServiceBusinessRulesTest {

    @Test
    fun `submitPersonalInformation blocks resubmit when country or nationality differs`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val service = KycService(repo, storage, email)

        val userId = ObjectId.get()

        val existing = KycVerification(
            userId = userId,
            country = "NP",
            nationality = "NP",
            firstName = "A",
            middleName = null,
            lastName = "B",
            dateOfBirth = LocalDate.parse("2000-01-01"),
            gender = KycResponse.Gender.MALE,
            fatherName = null,
            grandFatherName = null,
            maritalStatus = null,
        )

        whenever(repo.findByUserId(userId)).thenReturn(existing)

        assertThrows<org.apache.coyote.BadRequestException> {
            service.submitPersonalInformation(
                userId,
                PersonalInformation(
                    country = "US", // different
                    nationality = "NP",
                    firstName = "A",
                    middleName = null,
                    lastName = "B",
                    dateOfBirth = kotlinx.datetime.LocalDate.parse("2000-01-01"),
                    gender = KycResponse.Gender.MALE,
                    fatherName = null,
                    grandFatherName = null,
                    maritalStatus = null,
                )
            )
        }

        verify(repo, never()).save(any())
    }

    @Test
    fun `approve updates status and emails only if user has email`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val service = KycService(repo, storage, email)

        val userId = ObjectId.get()
        val existing = KycVerification(
            userId = userId,
            country = "NP",
            nationality = "NP",
            firstName = "A",
            middleName = null,
            lastName = "B",
            dateOfBirth = LocalDate.parse("2000-01-01"),
            gender = KycResponse.Gender.MALE,
            fatherName = null,
            grandFatherName = null,
            maritalStatus = null,
            status = KycResponse.Status.PENDING,
        )
        whenever(repo.findById(userId)).thenReturn(Optional.of(existing))
        whenever(repo.save(any())).thenAnswer { it.arguments[0] as KycVerification }

        val res = service.approve(
            baseUrl = "http://localhost",
            user = User(
                id = userId,
                firstName = "A",
                lastName = "B",
                email = null,
                phoneNumber = "+10000000000",
            )
        )

        assertEquals(KycResponse.Status.APPROVED, res.status)
        verify(email, never()).sendHtmlMail(
            baseUrl = any(),
            to = any(),
            subject = any(),
            template = any(),
            model = any()
        )
    }
}
