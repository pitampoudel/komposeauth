package pitampoudel.komposeauth.kyc

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.entity.User
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class KycServiceBusinessRulesTest {

    @Test
    fun `approve updates status and emails only if user has email`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val service = KycService(repo, storage, email, userRepository = mock())

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
