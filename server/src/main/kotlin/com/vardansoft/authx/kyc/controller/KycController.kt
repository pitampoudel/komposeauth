package com.vardansoft.authx.kyc.controller

import com.vardansoft.authx.core.config.UserContextService
import com.vardansoft.authx.data.ApiEndpoints
import com.vardansoft.authx.data.UpdateKycRequest
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.kyc.service.KycService
import io.swagger.v3.oas.annotations.Operation
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/${ApiEndpoints.KYC}")
class KycController(
    private val userContextService: UserContextService,
    private val kycService: KycService,
) {

    @Operation(
        summary = "Get KYC information for current user",
        description = "Retrieves the Know Your Customer (KYC) information for the currently authenticated user."
    )
    @GetMapping
    fun getMine(): ResponseEntity<KycResponse> {
        val user = userContextService.getCurrentUser()
        val kyc = kycService.find(user.id)
        return if (kyc != null) ResponseEntity.ok(kyc) else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Submit KYC information",
        description = "Submits or updates the Know Your Customer (KYC) information for the currently authenticated user."
    )
    @PostMapping
    fun submit(@Validated @RequestBody data: UpdateKycRequest): ResponseEntity<KycResponse> {
        val user = userContextService.getCurrentUser()
        return ResponseEntity.ok(kycService.submit(user.id, data))
    }

    @Operation(
        summary = "Approve KYC",
        description = "Approves a KYC record by its ID. Requires ADMIN role."
    )
    @PostMapping("/{kycId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    fun approve(@PathVariable kycId: String): ResponseEntity<KycResponse> = ResponseEntity.ok(
        kycService.approve(ObjectId(kycId))
    )

    @Operation(
        summary = "Reject KYC",
        description = "Rejects a KYC record by its ID, with an optional reason. Requires ADMIN role."
    )
    @PostMapping("/{kycId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun reject(
        @PathVariable kycId: String,
        @RequestParam(required = false) reason: String?
    ): ResponseEntity<KycResponse> = ResponseEntity.ok(
        kycService.reject(ObjectId(kycId), reason)
    )
}
