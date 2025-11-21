package pitampoudel.komposeauth.kyc.service

import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pitampoudel.core.data.EncodedData
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.data.DocumentInformation
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.PersonalInformation
import pitampoudel.komposeauth.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.kyc.dto.toResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.user.entity.User


@Service
class KycService(
    private val kycRepo: KycVerificationRepository,
    private val storageService: StorageService,
    val emailService: EmailService
) {

    fun find(userId: ObjectId): KycResponse? = kycRepo.findByUserId(userId)?.toResponse()

    fun isVerified(userId: ObjectId): Boolean {
        return find(userId)?.status == KycResponse.Status.APPROVED
    }

    fun getPending(): List<KycResponse> =
        kycRepo.findAllByStatus(KycResponse.Status.PENDING).map { it.toResponse() }

    @Transactional
    fun submitPersonalInformation(
        userId: ObjectId,
        data: PersonalInformation
    ): KycResponse {
        val existing = kycRepo.findByUserId(userId)

        if (existing != null && (existing.country != data.country || existing.nationality != data.nationality)) {
            throw BadRequestException("KYC already submitted with different country or nationality; cannot resubmit")
        }

        val newKycData = existing?.copy(
            country = data.country,
            nationality = data.nationality,
            firstName = data.firstName,
            middleName = data.middleName,
            lastName = data.lastName,
            dateOfBirth = data.dateOfBirth,
            gender = data.gender,
            fatherName = data.fatherName,
            grandFatherName = data.grandFatherName,
            motherName = data.motherName,
            grandMotherName = data.grandMotherName,
            maritalStatus = data.maritalStatus,
            occupation = data.occupation,
            pan = data.pan,
            email = data.email
        ) ?: KycVerification(
            userId = userId,
            country = data.country,
            nationality = data.nationality,
            firstName = data.firstName,
            middleName = data.middleName,
            lastName = data.lastName,
            dateOfBirth = data.dateOfBirth,
            gender = data.gender,
            fatherName = data.fatherName,
            grandFatherName = data.grandFatherName,
            motherName = data.motherName,
            grandMotherName = data.grandMotherName,
            maritalStatus = data.maritalStatus,
            occupation = data.occupation,
            pan = data.pan,
            email = data.email
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
            documentIssuedDate = data.documentIssuedDate,
            documentExpiryDate = data.documentExpiryDate,
            documentIssuedPlace = data.documentIssuedPlace,
            documentFrontUrl = newFrontUrl,
            documentBackUrl = newBackUrl,
            selfieUrl = newSelfieUrl,
            status = KycResponse.Status.PENDING
        )
        return kycRepo.save(entity).toResponse()
    }

    @Transactional
    fun approve(user: User): KycResponse {
        val res = updateStatus(user.id, KycResponse.Status.APPROVED)
        user.email?.let {
            val sent = emailService.sendSimpleMail(
                to = it,
                subject = "Your KYC has been approved",
                text = "Congratulations! Your KYC has been approved."
            )
        }
        return res
    }


    @Transactional
    fun reject(user: User, reason: String?): KycResponse {
        val res = updateStatus(user.id, KycResponse.Status.REJECTED)
        user.email?.let {
            val sent = emailService.sendSimpleMail(
                to = it,
                subject = "Your KYC has been rejected",
                text = "We are sorry to inform you that your KYC has been rejected. Reason: $reason"
            )
        }
        return res
    }

    private fun updateStatus(
        kycId: ObjectId,
        status: KycResponse.Status,
    ): KycResponse =
        kycRepo.findById(kycId).map { current ->
            kycRepo.save(current.copy(status = status)).toResponse()
        }.orElseThrow { IllegalArgumentException("KYC not found") }
}
