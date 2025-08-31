package com.vardansoft.authx.kyc.controller

import com.vardansoft.authx.core.config.UserContextService
import com.vardansoft.authx.kyc.dto.CreateKycRequest
import com.vardansoft.authx.kyc.dto.KycResponse
import com.vardansoft.authx.kyc.service.KycService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/kyc")
class KycController(
    private val userContextService: UserContextService,
    private val kycService: KycService,
) {

    @GetMapping
    fun getMine(): ResponseEntity<KycResponse> {
        val user = userContextService.getCurrentUser()
        val kyc = kycService.find(user.id)
        return if (kyc != null) ResponseEntity.ok(kyc) else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun submit(@Validated @RequestBody data: CreateKycRequest): ResponseEntity<KycResponse> {
        val user = userContextService.getCurrentUser()
        return ResponseEntity.ok(kycService.submit(user.id, data))
    }

    @PostMapping("/{kycId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    fun approve(@PathVariable kycId: String): ResponseEntity<KycResponse> =
        ResponseEntity.ok(kycService.approve(ObjectId(kycId)))

    @PostMapping("/{kycId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun reject(
        @PathVariable kycId: String,
        @RequestParam(required = false) reason: String?
    ): ResponseEntity<KycResponse> =
        ResponseEntity.ok(kycService.reject(ObjectId(kycId), reason))
}
