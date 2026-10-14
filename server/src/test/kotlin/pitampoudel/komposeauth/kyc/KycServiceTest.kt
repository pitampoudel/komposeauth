package pitampoudel.komposeauth.kyc

import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pitampoudel.core.data.EncodedData
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.storage.service.StorageService
import pitampoudel.komposeauth.core.service.SlackNotifier
import pitampoudel.komposeauth.kyc.data.DocumentInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.kyc.domain.DocumentType
import pitampoudel.komposeauth.kyc.entity.KycDocument
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class KycServiceTest {

    private fun baseKyc(userId: ObjectId, status: KycResponse.Status = KycResponse.Status.DRAFT) = KycVerification(
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
        status = status,
    )

    @Test
    fun `isVerified returns true only for APPROVED`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()

        whenever(repo.findByUserId(userId)).thenReturn(baseKyc(userId, status = KycResponse.Status.PENDING))
        assertEquals(false, service.isVerified(userId))

        whenever(repo.findByUserId(userId)).thenReturn(baseKyc(userId, status = KycResponse.Status.APPROVED))
        assertEquals(true, service.isVerified(userId))
    }

    @Test
    fun `submitPersonalInformation creates new record when none exists`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()
        whenever(repo.findByUserId(userId)).thenReturn(null)

        val captor = argumentCaptor<KycVerification>()
        whenever(repo.save(captor.capture())).thenAnswer { it.arguments[0] as KycVerification }

        val res = service.submitPersonalInformation(
            userId,
            PersonalInformation(
                country = "NP",
                nationality = "NP",
                firstName = "John",
                middleName = "M",
                lastName = "Doe",
                dateOfBirth = kotlinx.datetime.LocalDate.parse("1999-12-31"),
                gender = KycResponse.Gender.OTHER,
                fatherName = "Dad",
                grandFatherName = "Grand",
                maritalStatus = KycResponse.MaritalStatus.UNMARRIED,
            )
        )

        assertEquals(userId.toHexString(), res.userId)
        assertEquals(KycResponse.Status.DRAFT, res.status)
        assertEquals("John", captor.firstValue.firstName)
    }

    @Test
    fun `submitAddressDetails requires existing kyc`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()
        whenever(repo.findByUserId(userId)).thenReturn(null)

        assertThrows<BadRequestException> {
            service.submitAddressDetails(
                userId,
                UpdateAddressDetailsRequest(
                    currentAddress = pitampoudel.komposeauth.core.data.AddressInformation(null, null, null, null, null),
                    permanentAddress = pitampoudel.komposeauth.core.data.AddressInformation(null, null, null, null, null),
                )
            )
        }

        verify(repo, never()).save(any())
    }

    @Test
    fun `submitDocumentDetails uploads three docs sets status PENDING and stores urls`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()
        val existing = baseKyc(userId, status = KycResponse.Status.DRAFT)
        whenever(repo.findByUserId(userId)).thenReturn(existing)

        whenever(storage.upload(eq("kyc/${userId.toHexString()}/front"), any(), any())).thenReturn("front-url")
        whenever(storage.upload(eq("kyc/${userId.toHexString()}/back"), any(), any())).thenReturn("back-url")
        whenever(storage.upload(eq("kyc/${userId.toHexString()}/selfie"), any(), any())).thenReturn("selfie-url")

        val captor = argumentCaptor<KycVerification>()
        whenever(repo.replaceIfStatus(eq(KycResponse.Status.DRAFT), captor.capture()))
            .thenAnswer { it.arguments[1] as KycVerification }

        val encoded = EncodedData(base64EncodedData = "aGVsbG8=", mimeType = "image/png", name = "doc.png")
        val res = service.submitDocumentDetails(
            userId,
            DocumentInformation(
                documentType = DocumentType.NATIONAL_ID,
                documentNumber = "ABC123",
                documentIssuedDate = kotlinx.datetime.LocalDate.parse("2010-01-01"),
                documentExpiryDate = null,
                documentIssuedPlace = "Kathmandu",
                documentFront = encoded,
                documentBack = encoded,
                selfie = encoded,
            )
        )

        assertEquals(KycResponse.Status.PENDING, res.status)
        assertEquals("front-url", captor.firstValue.documentFrontUrl)
        assertEquals("back-url", captor.firstValue.documentBackUrl)
        assertEquals("selfie-url", captor.firstValue.selfieUrl)
        assertEquals("ABC123", captor.firstValue.documentNumber)
        assertEquals(DocumentType.NATIONAL_ID, captor.firstValue.documentType)

        // What reaches the bucket is sealed, not the file, and is typed as nothing a browser renders.
        val stored = argumentCaptor<ByteArray>()
        verify(storage).upload(eq("kyc/${userId.toHexString()}/front"), eq("application/octet-stream"), stored.capture())
        assertFalse(stored.firstValue.decodeToString().contains("hello"))

        // And the storage address is never handed out: the response names this server's.
        assertEquals("$TEST_ISSUER/kyc/${userId.toHexString()}/documents/front", res.documentInformation.documentFrontUrl)
        assertEquals("$TEST_ISSUER/kyc/${userId.toHexString()}/documents/back", res.documentInformation.documentBackUrl)
        assertEquals("$TEST_ISSUER/kyc/${userId.toHexString()}/documents/selfie", res.documentInformation.selfieUrl)
    }

    @Test
    fun `a submitted document reads back as it was sent, and only as the file it was stored as`() {
        val repo = mock<KycVerificationRepository>()
        val bucket = mutableMapOf<String, ByteArray>()
        val storage = mock<StorageService> {
            on { upload(any(), any(), any()) } doAnswer { call ->
                "bucket://${call.getArgument<String>(0)}".also { bucket[it] = call.getArgument(2) }
            }
            on { download(any()) } doAnswer { call -> bucket[call.getArgument<String>(0)] }
        }
        val service = KycService(repo, kycDocumentsOver(storage), mock(), mock(), mock<SlackNotifier>())
        val userId = ObjectId.get()
        whenever(repo.findByUserId(userId)).thenReturn(baseKyc(userId))
        whenever(repo.replaceIfStatus(any(), any())).thenAnswer { it.arguments[1] }

        fun encoded(text: String) = EncodedData(
            base64EncodedData = java.util.Base64.getEncoder().encodeToString(text.toByteArray()),
            mimeType = "image/png",
            name = "$text.png"
        )
        service.submitDocumentDetails(
            userId,
            DocumentInformation(
                documentType = DocumentType.NATIONAL_ID,
                documentNumber = "ABC123",
                documentIssuedDate = kotlinx.datetime.LocalDate.parse("2010-01-01"),
                documentExpiryDate = null,
                documentIssuedPlace = "Kathmandu",
                documentFront = encoded("front side"),
                documentBack = encoded("back side"),
                selfie = encoded("a face"),
            )
        )
        val saved = argumentCaptor<KycVerification>().also { verify(repo).replaceIfStatus(any(), it.capture()) }.firstValue
        whenever(repo.findByUserId(userId)).thenReturn(saved)

        assertEquals("front side", service.document(userId, KycDocument.FRONT)?.decodeToString())
        assertEquals("a face", service.document(userId, KycDocument.SELFIE)?.decodeToString())

        // A sealed file moved under another name does not open there.
        whenever(repo.findByUserId(userId)).thenReturn(saved.copy(selfieUrl = saved.documentFrontUrl))
        assertThrows<javax.crypto.AEADBadTagException> { service.document(userId, KycDocument.SELFIE) }
    }

    @Test
    fun `submitDocumentDetails blocks when already submitted`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()
        whenever(repo.findByUserId(userId)).thenReturn(baseKyc(userId, status = KycResponse.Status.PENDING))

        assertThrows<BadRequestException> {
            service.submitDocumentDetails(
                userId,
                DocumentInformation(
                    documentType = DocumentType.PASSPORT,
                    documentNumber = "P123",
                    documentIssuedDate = kotlinx.datetime.LocalDate.parse("2010-01-01"),
                    documentExpiryDate = kotlinx.datetime.LocalDate.parse("2030-01-01"),
                    documentIssuedPlace = "Kathmandu",
                    documentFront = EncodedData(base64EncodedData = "aGVsbG8=", mimeType = "image/png", name = "front.png"),
                    documentBack = EncodedData(base64EncodedData = "aGVsbG8=", mimeType = "image/png", name = "back.png"),
                    selfie = EncodedData(base64EncodedData = "aGVsbG8=", mimeType = "image/png", name = "selfie.png"),
                )
            )
        }
        verify(storage, never()).upload(any(), any(), any())
    }

    @Test
    fun `reject emails only if user has email and includes reason when provided`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()
        val existing = baseKyc(userId, status = KycResponse.Status.PENDING)
        whenever(repo.findById(userId)).thenReturn(Optional.of(existing))
        whenever(repo.replaceIfStatus(eq(KycResponse.Status.PENDING), any())).thenAnswer { it.arguments[1] as KycVerification }

        val user = User(
            id = userId,
            firstName = "A",
            lastName = "B",
            email = "a@example.com",
            phoneNumber = "+10000000000",
        )

        val res = service.reject(baseUrl = "http://localhost", user = user, reason = "Blurry photo")
        assertEquals(KycResponse.Status.REJECTED, res.status)

        val modelCaptor = argumentCaptor<Map<String, Any>>()
        verify(email).sendHtmlMail(
            baseUrl = any(),
            to = eq("a@example.com"),
            subject = any(),
            template = any(),
            model = modelCaptor.capture(),
        )
        val message = modelCaptor.firstValue["message"] as String
        assertNotNull(message)
        assertEquals(true, message.contains("Reason: Blurry photo"))
    }

    @Test
    fun `approve copies missing user names from kyc and uses them in email`() {
        val repo = mock<KycVerificationRepository>()
        val storage = mock<StorageService>()
        val email = mock<EmailService>()
        val userRepo = mock<UserRepository>()
        val service = KycService(repo, kycDocumentsOver(storage), email, userRepo, mock<SlackNotifier>())

        val userId = ObjectId.get()
        val kyc = baseKyc(userId, status = KycResponse.Status.PENDING)
        whenever(repo.findById(userId)).thenReturn(Optional.of(kyc))
        whenever(repo.replaceIfStatus(eq(KycResponse.Status.PENDING), any())).thenAnswer { it.arguments[1] as KycVerification }
        val savedUser = argumentCaptor<User>()
        whenever(userRepo.save(savedUser.capture())).thenAnswer { it.arguments[0] as User }

        val user = User(
            id = userId,
            firstName = null,
            lastName = null,
            email = "missing@example.com",
            phoneNumber = "+10000000000",
        )

        val modelCaptor = argumentCaptor<Map<String, Any>>()
        service.approve(baseUrl = "http://localhost", user = user)

        assertEquals(kyc.firstName, savedUser.firstValue.firstName)
        assertEquals(kyc.lastName, savedUser.firstValue.lastName)

        verify(email).sendHtmlMail(
            baseUrl = any(),
            to = eq("missing@example.com"),
            subject = any(),
            template = any(),
            model = modelCaptor.capture()
        )
        assertEquals(kyc.firstName, modelCaptor.firstValue["recipientName"])
    }

    @Test
    fun `a KYC that is not waiting for review cannot be approved or rejected`() {
        val repo = mock<KycVerificationRepository>()
        val email = mock<EmailService>()
        val service = KycService(repo, kycDocumentsOver(mock()), email, mock<UserRepository>(), mock<SlackNotifier>())
        val user = User(id = ObjectId.get(), firstName = "A", lastName = "B", email = "a@example.com", phoneNumber = null)

        listOf(KycResponse.Status.DRAFT, KycResponse.Status.APPROVED, KycResponse.Status.REJECTED).forEach { status ->
            whenever(repo.findById(user.id)).thenReturn(Optional.of(baseKyc(user.id, status = status)))
            assertThrows<BadRequestException> { service.approve(baseUrl = "http://localhost", user = user) }
            assertThrows<BadRequestException> { service.reject(baseUrl = "http://localhost", user = user, reason = null) }
        }
        verify(repo, never()).replaceIfStatus(any(), any())
        verify(email, never()).sendHtmlMail(any(), any(), any(), any(), any())
    }

    @Test
    fun `a decision another reviewer got to first is refused and mails nobody`() {
        val repo = mock<KycVerificationRepository>()
        val email = mock<EmailService>()
        val service = KycService(repo, kycDocumentsOver(mock()), email, mock<UserRepository>(), mock<SlackNotifier>())
        val user = User(id = ObjectId.get(), firstName = "A", lastName = "B", email = "a@example.com", phoneNumber = null)
        // Read as waiting for review, decided by somebody else before this decision is stored.
        whenever(repo.findById(user.id)).thenReturn(Optional.of(baseKyc(user.id, status = KycResponse.Status.PENDING)))
        whenever(repo.replaceIfStatus(any(), any())).thenReturn(null)

        assertThrows<BadRequestException> { service.approve(baseUrl = "http://localhost", user = user) }
        assertThrows<BadRequestException> { service.reject(baseUrl = "http://localhost", user = user, reason = null) }
        verify(repo, never()).save(any())
        verify(email, never()).sendHtmlMail(any(), any(), any(), any(), any())
    }

    @Test
    fun `an edit read before a submission landed does not overwrite it`() {
        val repo = mock<KycVerificationRepository>()
        val service = KycService(repo, kycDocumentsOver(mock()), mock(), mock<UserRepository>(), mock<SlackNotifier>())
        val userId = ObjectId.get()
        val draft = baseKyc(userId, status = KycResponse.Status.DRAFT)
        whenever(repo.findByUserId(userId)).thenReturn(draft)
        // Submitted for review between this edit's read and its save.
        whenever(repo.replaceIfStatus(any(), any())).thenReturn(null)

        assertThrows<BadRequestException> {
            service.submitPersonalInformation(
                userId,
                PersonalInformation(
                    country = "NP",
                    nationality = "NP",
                    firstName = "Changed",
                    middleName = null,
                    lastName = "B",
                    dateOfBirth = kotlinx.datetime.LocalDate.parse("2000-01-01"),
                    gender = KycResponse.Gender.MALE,
                    fatherName = null,
                    grandFatherName = null,
                    maritalStatus = null
                )
            )
        }
        verify(repo).replaceIfStatus(eq(KycResponse.Status.DRAFT), any())
        verify(repo, never()).save(any())
    }
}
