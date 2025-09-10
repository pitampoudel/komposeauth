package com.vardansoft.authx.kyc.service

import com.vardansoft.authx.core.service.StorageService
import com.vardansoft.authx.data.UpdateKycRequest
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.kyc.dto.toResponse
import com.vardansoft.authx.kyc.entity.KycVerification
import com.vardansoft.authx.kyc.repository.KycVerificationRepository
import com.vardansoft.core.data.EncodedData
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
    fun submit(userId: ObjectId, data: UpdateKycRequest): KycResponse {
        val existing = kycRepo.findByUserId(userId)

        fun uploadIfPresent(label: String, encoded: EncodedData?): String? {
            if (encoded == null) return null
            val file = encoded.toKmpFile()
            val blobName = "kyc/${userId.toHexString()}/$label"
            return storage.upload(blobName, file.mimeType, file.byteArray)
        }

        val newFrontUrl = uploadIfPresent("front", data.documentFront)
        val newBackUrl = uploadIfPresent("back", data.documentBack)
        val newSelfieUrl = uploadIfPresent("selfie", data.selfie)

        val entity = if (existing == null) {
            KycVerification(
                userId = userId,
                nationality = data.nationality,
                firstName = data.firstName,
                middleName = data.middleName,
                lastName = data.lastName,
                dateOfBirth = data.dateOfBirth,
                gender = data.gender,
                fatherName = data.fatherName,
                motherName = data.motherName,
                maritalStatus = data.maritalStatus,
                documentType = data.documentType,
                documentNumber = data.documentNumber,
                documentIssuedDate = data.documentIssuedDate,
                documentExpiryDate = data.documentExpiryDate,
                documentIssuedPlace = data.documentIssuedPlace,
                status = KycResponse.Status.PENDING,
                remarks = null,
                documentFrontUrl = newFrontUrl,
                documentBackUrl = newBackUrl,
                selfieUrl = newSelfieUrl
            )
        } else {
            if (existing.status == KycResponse.Status.APPROVED) {
                throw IllegalStateException("KYC already approved; cannot resubmit")
            }
            existing.copy(
                nationality = data.nationality,
                firstName = data.firstName,
                middleName = data.middleName,
                lastName = data.lastName,
                dateOfBirth = data.dateOfBirth,
                gender = data.gender,
                fatherName = data.fatherName,
                motherName = data.motherName,
                maritalStatus = data.maritalStatus,
                documentType = data.documentType,
                documentNumber = data.documentNumber,
                documentIssuedDate = data.documentIssuedDate,
                documentExpiryDate = data.documentExpiryDate,
                documentIssuedPlace = data.documentIssuedPlace,
                documentFrontUrl = newFrontUrl ?: existing.documentFrontUrl,
                documentBackUrl = newBackUrl ?: existing.documentBackUrl,
                selfieUrl = newSelfieUrl ?: existing.selfieUrl,
                status = KycResponse.Status.PENDING,
                remarks = null
            )
        }
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
