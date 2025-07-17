package com.vardansoft.auth.credentials.service

import com.vardansoft.auth.credentials.dto.CredentialResponse
import com.vardansoft.auth.credentials.dto.UpdateCredentialRequest
import com.vardansoft.auth.credentials.entity.Credential
import com.vardansoft.auth.credentials.repository.CredentialRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class CredentialService(
    private val credentialRepository: CredentialRepository
) {

    fun findCredentials(userId: ObjectId): List<CredentialResponse> {
        return credentialRepository.findByUserId(userId).map { it.mapToResponseDto() }
    }

    fun updateCredential(userId: ObjectId, req: UpdateCredentialRequest): CredentialResponse {
        val id = credentialRepository.findByUserId(userId).find { it.provider == req.provider }?.id ?: ObjectId()
        val savedCredential = credentialRepository.save(
            Credential(
                id = id,
                userId = userId,
                provider = req.provider,
                accessToken = req.accessToken,
                refreshToken = req.refreshToken
            )
        )
        return savedCredential.mapToResponseDto()
    }

    private fun Credential.mapToResponseDto(): CredentialResponse {
        return CredentialResponse(
            provider = this.provider,
            accessToken = this.accessToken,
            refreshToken = this.refreshToken,
        )
    }
}