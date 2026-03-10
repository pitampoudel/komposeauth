package pitampoudel.komposeauth.kyc.service

import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pitampoudel.core.data.EncodedData
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.SlackNotifier
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.kyc.data.DocumentInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import pitampoudel.komposeauth.kyc.dto.toResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import java.time.Instant
import java.util.Base64
import kotlinx.datetime.toJavaLocalDate
import pitampoudel.komposeauth.core.utils.detectMimeType
import pitampoudel.komposeauth.kyc.domain.DocumentType


@Service
class KycService(
    private val kycRepo: KycVerificationRepository,
    private val storageService: StorageService,
    val emailService: EmailService,
    private val userRepository: UserRepository,
    private val slackNotifier: SlackNotifier
) {

    fun find(userId: ObjectId): KycResponse? = kycRepo.findByUserId(userId)?.toResponse()

    fun isVerified(userId: ObjectId): Boolean {
        return find(userId)?.status == KycResponse.Status.APPROVED
    }

    fun verifiedUserIds(userIds: Collection<ObjectId>): Set<ObjectId> {
        if (userIds.isEmpty()) return emptySet()
        return kycRepo.findAllById(userIds)
            .asSequence()
            .filter { it.status == KycResponse.Status.APPROVED }
            .map { it.userId }
            .toSet()
    }

    fun getPending(): List<KycResponse> =
        kycRepo.findAllByStatus(KycResponse.Status.PENDING).map { it.toResponse() }

    @Transactional
    fun submitPersonalInformation(
        userId: ObjectId,
        data: PersonalInformation
    ): KycResponse {
        val existing = kycRepo.findByUserId(userId)

        val newKycData = existing?.copy(
            country = data.country,
            nationality = data.nationality,
            firstName = data.firstName,
            middleName = data.middleName,
            lastName = data.lastName,
            dateOfBirth = data.dateOfBirth.toJavaLocalDate(),
            gender = data.gender,
            fatherName = data.fatherName,
            grandFatherName = data.grandFatherName,
            maritalStatus = data.maritalStatus
        ) ?: KycVerification(
            userId = userId,
            country = data.country,
            nationality = data.nationality,
            firstName = data.firstName,
            middleName = data.middleName,
            lastName = data.lastName,
            dateOfBirth = data.dateOfBirth.toJavaLocalDate(),
            gender = data.gender,
            fatherName = data.fatherName,
            grandFatherName = data.grandFatherName,
            maritalStatus = data.maritalStatus
        )

        if (existing?.status in KycResponse.Status.submitted() && existing != newKycData) {
            throw BadRequestException("KYC already submitted; cannot resubmit")
        }

        return kycRepo.save(newKycData).toResponse()
    }

    @Transactional
    fun submitAddressDetails(userId: ObjectId, data: UpdateAddressDetailsRequest): KycResponse {
        val existing = kycRepo.findByUserId(userId) ?: throw BadRequestException("KYC not found")
        val newKycData = existing.copy(
            permanentAddressCountry = data.permanentAddress.country,
            permanentAddressState = data.permanentAddress.state,
            permanentAddressCity = data.permanentAddress.city,
            permanentAddressLine1 = data.permanentAddress.addressLine1,
            permanentAddressLine2 = data.permanentAddress.addressLine2,
            currentAddressCountry = data.currentAddress.country,
            currentAddressState = data.currentAddress.state,
            currentAddressCity = data.currentAddress.city,
            currentAddressLine1 = data.currentAddress.addressLine1,
            currentAddressLine2 = data.currentAddress.addressLine2,
        )
        if (existing.status in KycResponse.Status.submitted() && existing != newKycData) {
            throw BadRequestException("KYC already submitted; cannot resubmit")
        }

        return kycRepo.save(newKycData).toResponse()
    }

    @Transactional
    fun submitDocumentDetails(userId: ObjectId, data: DocumentInformation): KycResponse {
        val existing = kycRepo.findByUserId(userId) ?: throw BadRequestException("KYC not found")

        if (existing.status in KycResponse.Status.submitted()) {
            throw BadRequestException("KYC already submitted; cannot resubmit")
        }

        fun uploadKycDoc(label: String, encoded: EncodedData): String {
            val file = encoded.toKmpFile()
            val blobName = "kyc/${userId.toHexString()}/$label"
            return storageService.upload(blobName, file.mimeType, file.byteArray)
        }

        val newFrontUrl = uploadKycDoc("front", data.documentFront)
        val newBackUrl = uploadKycDoc("back", data.documentBack)
        val newSelfieUrl = uploadKycDoc("selfie", data.selfie)

        val entity = existing.copy(
            documentType = data.documentType,
            documentNumber = data.documentNumber,
            documentIssuedDate = data.documentIssuedDate.toJavaLocalDate(),
            documentExpiryDate = data.documentExpiryDate?.toJavaLocalDate(),
            documentIssuedPlace = data.documentIssuedPlace,
            documentFrontUrl = newFrontUrl,
            documentBackUrl = newBackUrl,
            selfieUrl = newSelfieUrl,
            status = KycResponse.Status.PENDING
        )
        return kycRepo.save(entity).toResponse()
    }

    @Transactional
    fun approve(baseUrl: String, user: User): KycResponse {
        val enrichedUser = user.takeIf { it.firstName != null && it.lastName != null } ?: run {
            val kyc = kycRepo.findById(user.id).orElse(null)
            if (kyc != null) {
                val updated = user.copy(
                    firstName = user.firstName ?: kyc.firstName,
                    lastName = user.lastName ?: kyc.lastName,
                    updatedAt = Instant.now()
                )
                if (updated != user) userRepository.save(updated) else user
            } else user
        }

        val res = updateStatus(user.id, KycResponse.Status.APPROVED)
        enrichedUser.email?.let {
            emailService.sendHtmlMail(
                baseUrl = baseUrl,
                to = it,
                subject = "Your KYC has been approved",
                template = "email/generic",
                model = mapOf(
                    "recipientName" to enrichedUser.firstNameOrUser(),
                    "message" to "Congratulations! Your KYC has been approved."
                )
            )
        }
        return res
    }


    @Transactional
    fun reject(baseUrl: String, user: User, reason: String?): KycResponse {
        val res = updateStatus(user.id, KycResponse.Status.REJECTED)
        user.email?.let { userEmail ->
            emailService.sendHtmlMail(
                baseUrl = baseUrl,
                to = userEmail,
                subject = "Your KYC has been rejected",
                template = "email/generic",
                model = mapOf(
                    "recipientName" to user.firstNameOrUser(),
                    "message" to ("We are sorry to inform you that your KYC has been rejected." + (reason?.let { " Reason: $it" }
                        ?: ""))
                )
            )
        }
        return res
    }

    @Transactional
    fun submitThirdFactorVerification(baseUrl: String, data: ThirdFactorModel): KycResponse {

        val userId = runCatching { ObjectId(data.identifier) }.getOrElse {
            throw BadRequestException("Invalid user identifier: ${data.identifier}")
        }
        val existingKyc = kycRepo.findByUserId(userId) ?: throw BadRequestException("KYC not found")

        if (existingKyc.status in KycResponse.Status.submitted()) {
            throw BadRequestException("KYC already submitted; cannot resubmit")
        }

        val detectedDocType = data.documentDetectionLog.lastOrNull { it.isVerified }?.claimedDocType?.let {
            when {
                it.contains("citizenship", ignoreCase = true) -> DocumentType.CITIZENSHIP
                it.contains("passport", ignoreCase = true) -> DocumentType.PASSPORT
                it.contains("national", ignoreCase = true) -> DocumentType.NATIONAL_ID
                else -> null
            }
        }

        fun uploadBase64Photo(label: String, base64: String): String? {
            val raw = base64.substringAfter("base64,").trim().takeIf { it.isNotBlank() } ?: return null
            val bytes = runCatching { Base64.getDecoder().decode(raw) }.getOrNull() ?: return null
            return storageService.upload("kyc/${userId.toHexString()}/$label", bytes.detectMimeType(), bytes)
        }

        val documentFront =
            data.documentPhoto.lastOrNull { it.claimedDocType.contains("front", ignoreCase = true) }?.photo
        val documentBack =
            data.documentPhoto.lastOrNull { it.claimedDocType.contains("back", ignoreCase = true) }?.photo

        if (detectedDocType == null || data.userPhoto.isBlank() || documentFront == null || documentBack == null) {
            throw BadRequestException("Document detection failed")
        }

        val selfieUrl = uploadBase64Photo("selfie", data.userPhoto)
        val documentFrontUrl = uploadBase64Photo("front", documentFront)
        val documentBackUrl = uploadBase64Photo("back", documentBack)


        val updated = existingKyc.copy(
            documentType = detectedDocType,
            documentNumber = data.documentNumber,
            documentFrontUrl = documentFrontUrl,
            documentBackUrl = documentBackUrl,
            documentIssuedDate = null,
            documentExpiryDate = null,
            documentIssuedPlace = null,
            selfieUrl = selfieUrl,
            status = KycResponse.Status.PENDING
        )
        val saved = kycRepo.save(updated)
        val user = userRepository.findById(userId).orElse(null)
        slackNotifier.send("📝 KYC documents submitted by ${user.fullName}")
        return saved.toResponse()
    }


    private fun updateStatus(
        kycId: ObjectId,
        status: KycResponse.Status,
    ): KycResponse =
        kycRepo.findById(kycId).map { current ->
            kycRepo.save(current.copy(status = status)).toResponse()
        }.orElseThrow { IllegalArgumentException("KYC not found") }
}
