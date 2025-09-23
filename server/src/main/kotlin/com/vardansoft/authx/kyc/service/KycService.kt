package com.vardansoft.authx.kyc.service

import com.vardansoft.authx.core.service.StorageService
import com.vardansoft.authx.data.DocumentInformation
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.data.UpdateAddressDetailsRequest
import com.vardansoft.authx.kyc.dto.toResponse
import com.vardansoft.authx.kyc.entity.KycVerification
import com.vardansoft.authx.kyc.repository.KycVerificationRepository
import com.vardansoft.core.data.EncodedData
import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KycService(
    private val kycRepo: KycVerificationRepository,
    private val storage: StorageService,
) {

    fun find(userId: ObjectId): KycResponse? = kycRepo.findByUserId(userId)?.toResponse()

    @Transactional
    fun submitPersonalInformation(
        userId: ObjectId,
        data: PersonalInformation
    ): KycResponse {
        val existing = kycRepo.findByUserId(userId)

        if (existing?.status == KycResponse.Status.APPROVED) {
            throw IllegalStateException("KYC already approved; cannot resubmit")
        }

        val kycRecord = existing?.copy(
            nationality = data.nationality,
            firstName = data.firstName,
            middleName = data.middleName,
            lastName = data.lastName,
            dateOfBirth = data.dateOfBirth,
            gender = data.gender,
            fatherName = data.fatherName,
            motherName = data.motherName,
            maritalStatus = data.maritalStatus,
        ) ?: KycVerification(
            userId = userId,
            nationality = data.nationality,
            firstName = data.firstName,
            middleName = data.middleName,
            lastName = data.lastName,
            dateOfBirth = data.dateOfBirth,
            gender = data.gender,
            fatherName = data.fatherName,
            motherName = data.motherName,
            maritalStatus = data.maritalStatus
        )

        return kycRepo.save(kycRecord).toResponse()
    }

    @Transactional
    fun submitAddressDetails(userId: ObjectId, data: UpdateAddressDetailsRequest): KycResponse {
        val existing = kycRepo.findByUserId(userId) ?: throw BadRequestException("KYC not found")

        if (existing.status == KycResponse.Status.APPROVED) {
            throw IllegalStateException("KYC already approved; cannot resubmit")
        }

        val entity = existing.copy(
            permanentAddressCountry = data.permanentAddress.country,
            permanentAddressProvince = data.permanentAddress.province,
            permanentAddressDistrict = data.permanentAddress.district,
            permanentAddressLocalUnit = data.permanentAddress.localUnit,
            permanentAddressWardNo = data.permanentAddress.wardNo,
            permanentAddressTole = data.permanentAddress.tole,
            currentAddressCountry = data.currentAddress.country,
            currentAddressProvince = data.currentAddress.province,
            currentAddressDistrict = data.currentAddress.district,
            currentAddressLocalUnit = data.currentAddress.localUnit,
            currentAddressWardNo = data.currentAddress.wardNo,
            currentAddressTole = data.currentAddress.tole
        )
        return kycRepo.save(entity).toResponse()
    }

    @Transactional
    fun submitDocumentDetails(userId: ObjectId, data: DocumentInformation): KycResponse {
        val existing = kycRepo.findByUserId(userId) ?: throw BadRequestException("KYC not found")

        if (existing.status == KycResponse.Status.APPROVED) {
            throw IllegalStateException("KYC already approved; cannot resubmit")
        }

        fun upload(label: String, encoded: EncodedData): String {
            val file = encoded.toKmpFile()
            val blobName = "kyc/${userId.toHexString()}/$label"
            return storage.upload(blobName, file.mimeType, file.byteArray)
        }

        val newFrontUrl = upload("front", data.documentFront)
        val newBackUrl = upload("back", data.documentBack)
        val newSelfieUrl = upload("selfie", data.selfie)

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
    fun approve(kycId: ObjectId): KycResponse =
        updateStatus(kycId, KycResponse.Status.APPROVED, null)

    @Transactional
    fun reject(kycId: ObjectId, reason: String?): KycResponse =
        updateStatus(kycId, KycResponse.Status.REJECTED, reason)

    private fun updateStatus(
        kycId: ObjectId,
        status: KycResponse.Status,
        remarks: String?
    ): KycResponse =
        kycRepo.findById(kycId).map { current ->
            kycRepo.save(current.copy(status = status, remarks = remarks)).toResponse()
        }.orElseThrow { IllegalArgumentException("KYC not found") }
}
