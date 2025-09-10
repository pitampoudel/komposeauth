package com.vardansoft.authx.kyc.service

import com.vardansoft.authx.core.service.StorageService
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.UpdateKycRequest
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

        fun upload(label: String, encoded: EncodedData): String {
            val file = encoded.toKmpFile()
            val blobName = "kyc/${userId.toHexString()}/$label"
            return storage.upload(blobName, file.mimeType, file.byteArray)
        }

        val newFrontUrl = upload("front", data.documentInformation.documentFront)
        val newBackUrl = upload("back", data.documentInformation.documentBack)
        val newSelfieUrl = upload("selfie", data.documentInformation.selfie)

        val entity = if (existing == null) {
            KycVerification(
                userId = userId,
                nationality = data.personalInformation.nationality,
                firstName = data.personalInformation.firstName,
                middleName = data.personalInformation.middleName,
                lastName = data.personalInformation.lastName,
                dateOfBirth = data.personalInformation.dateOfBirth,
                gender = data.personalInformation.gender,
                fatherName = data.familyInformation.fatherName,
                motherName = data.familyInformation.motherName,
                maritalStatus = data.familyInformation.maritalStatus,
                documentType = data.documentInformation.documentType,
                documentNumber = data.documentInformation.documentNumber,
                documentIssuedDate = data.documentInformation.documentIssuedDate,
                documentExpiryDate = data.documentInformation.documentExpiryDate,
                documentIssuedPlace = data.documentInformation.documentIssuedPlace,
                documentFrontUrl = newFrontUrl,
                documentBackUrl = newBackUrl,
                selfieUrl = newSelfieUrl,
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
                currentAddressTole = data.currentAddress.tole,
                status = KycResponse.Status.PENDING,
                remarks = null,
            )
        } else {
            if (existing.status == KycResponse.Status.APPROVED) {
                throw IllegalStateException("KYC already approved; cannot resubmit")
            }
            existing.copy(
                nationality = data.personalInformation.nationality,
                firstName = data.personalInformation.firstName,
                middleName = data.personalInformation.middleName,
                lastName = data.personalInformation.lastName,
                dateOfBirth = data.personalInformation.dateOfBirth,
                gender = data.personalInformation.gender,
                fatherName = data.familyInformation.fatherName,
                motherName = data.familyInformation.motherName,
                maritalStatus = data.familyInformation.maritalStatus,
                documentType = data.documentInformation.documentType,
                documentNumber = data.documentInformation.documentNumber,
                documentIssuedDate = data.documentInformation.documentIssuedDate,
                documentExpiryDate = data.documentInformation.documentExpiryDate,
                documentIssuedPlace = data.documentInformation.documentIssuedPlace,
                documentFrontUrl = newFrontUrl,
                documentBackUrl = newBackUrl,
                selfieUrl = newSelfieUrl,
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
                currentAddressTole = data.currentAddress.tole,
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
