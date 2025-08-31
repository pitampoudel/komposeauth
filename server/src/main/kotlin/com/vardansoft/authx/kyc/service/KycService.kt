package com.vardansoft.authx.kyc.service

import com.vardansoft.authx.kyc.dto.CreateKycRequest
import com.vardansoft.authx.kyc.dto.KycResponse
import com.vardansoft.authx.kyc.dto.toResponse
import com.vardansoft.authx.kyc.entity.KycVerification
import com.vardansoft.authx.kyc.repository.KycVerificationRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KycService(
    private val kycRepo: KycVerificationRepository,
) {

    fun find(userId: ObjectId): KycResponse? = kycRepo.findByUserId(userId)?.toResponse()

    @Transactional
    fun submit(userId: ObjectId, data: CreateKycRequest): KycResponse {
        val existing = kycRepo.findByUserId(userId)
        val entity = if (existing == null) {
            KycVerification(
                userId = userId,
                fullName = data.fullName,
                documentType = data.documentType,
                documentNumber = data.documentNumber,
                country = data.country,
                documentFrontUrl = data.documentFrontUrl,
                documentBackUrl = data.documentBackUrl,
                selfieUrl = data.selfieUrl,
                status = KycVerification.Status.PENDING,
            )
        } else {
            if (existing.status == KycVerification.Status.APPROVED) {
                throw IllegalStateException("KYC already approved; cannot resubmit")
            }
            existing.copy(
                fullName = data.fullName,
                documentType = data.documentType,
                documentNumber = data.documentNumber,
                country = data.country,
                documentFrontUrl = data.documentFrontUrl,
                documentBackUrl = data.documentBackUrl,
                selfieUrl = data.selfieUrl,
                status = KycVerification.Status.PENDING,
                remarks = null
            )
        }
        return kycRepo.save(entity).toResponse()
    }

    @Transactional
    fun approve(kycId: ObjectId): KycResponse =
        updateStatus(kycId, KycVerification.Status.APPROVED, null)

    @Transactional
    fun reject(kycId: ObjectId, reason: String?): KycResponse =
        updateStatus(kycId, KycVerification.Status.REJECTED, reason)

    private fun updateStatus(kycId: ObjectId, status: KycVerification.Status, remarks: String?): KycResponse =
        kycRepo.findById(kycId).map { current ->
            kycRepo.save(current.copy(status = status, remarks = remarks)).toResponse()
        }.orElseThrow { IllegalArgumentException("KYC not found") }
}
