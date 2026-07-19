package pitampoudel.komposeauth.kyc

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.SlackNotifier
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.repository.UserRepository
import java.time.LocalDate
import java.util.Base64
import java.util.Optional
import kotlin.test.assertEquals

/**
 * The scanned document outranks what the user typed — but only where Third Factor actually managed
 * to read it.
 */
class ThirdFactorOverwriteTest {

    private val photo = Base64.getEncoder().encodeToString("image-bytes".toByteArray())
    private val userId = ObjectId.get()

    private val repo = mock<KycVerificationRepository>()
    private val storage = mock<StorageService>()
    private val userRepo = mock<UserRepository>()

    private val service = KycService(repo, storage, mock<EmailService>(), userRepo, mock<SlackNotifier>())

    /** Declared by the user: Nepali male. */
    private val declared = KycVerification(
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
        status = KycResponse.Status.DRAFT
    )

    private fun webhook(nationality: String?, gender: String?) = ThirdFactorModel(
        identifier = userId.toHexString(),
        jwt = "jwt",
        nationality = nationality,
        gender = gender,
        userPhoto = photo,
        documentDetectionLog = listOf(
            ThirdFactorModel.DocumentDetectionLog(isVerified = true, detectedDocType = "citizenship-front")
        ),
        documentPhoto = listOf(
            ThirdFactorModel.DocumentPhoto(detectedDocType = "citizenship-front", photo = photo),
            ThirdFactorModel.DocumentPhoto(detectedDocType = "citizenship-back", photo = photo)
        )
    )

    private fun submit(data: ThirdFactorModel): KycVerification {
        whenever(repo.findByUserId(userId)).thenReturn(declared)
        whenever(repo.save(any<KycVerification>())).thenAnswer { it.arguments[0] }
        whenever(storage.upload(any(), any(), any())).thenReturn("https://storage/x")
        whenever(userRepo.findById(userId)).thenReturn(Optional.empty())

        service.submitThirdFactorVerification("https://example.test", data)

        val captor = argumentCaptor<KycVerification>()
        verify(repo).save(captor.capture())
        return captor.firstValue
    }

    @Test
    fun `the document overwrites what the user declared`() {
        val saved = submit(webhook(nationality = "nepali", gender = "Female"))

        assertEquals("nepali", saved.nationality)
        assertEquals(KycResponse.Gender.FEMALE, saved.gender)
    }

    @Test
    fun `a scan that could not read the values leaves the declared ones intact`() {
        // Third Factor reports 'N/A' gender and omits nationality when detection fails.
        val saved = submit(webhook(nationality = null, gender = "N/A"))

        assertEquals("NP", saved.nationality)
        assertEquals(KycResponse.Gender.MALE, saved.gender)
    }

    @Test
    fun `a blank nationality does not wipe the declared one`() {
        val saved = submit(webhook(nationality = "  ", gender = null))

        assertEquals("NP", saved.nationality)
        assertEquals(KycResponse.Gender.MALE, saved.gender)
    }
}
