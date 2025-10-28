package pitampoudel.komposeauth.kyc.controller

import io.swagger.v3.oas.annotations.Operation
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
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.DocumentInformation
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.PersonalInformation
import pitampoudel.komposeauth.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService
import javax.security.auth.login.AccountNotFoundException

@RestController
@RequestMapping("/${ApiEndpoints.KYC}")
class KycController(
    val userService: UserService,
    private val userContextService: UserContextService,
    private val kycService: KycService,
) {

    @Operation(
        summary = "Get KYC information for current user",
        description = "Retrieves the Know Your Customer (KYC) information for the currently authenticated user."
    )
    @GetMapping
    fun getMine(): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        val kyc = kycService.find(user.id)
        return if (kyc != null) ResponseEntity.ok(kyc) else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Get all pending KYC submissions",
        description = "Retrieves all KYC submissions with a 'PENDING' status. Requires ADMIN role."
    )
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPending(): ResponseEntity<List<KycResponse>> = ResponseEntity.ok(kycService.getPending())

    @Operation(
        summary = "Submit Personal Information",
        description = "Submits or updates the Personal information for the currently authenticated user."
    )
    @PostMapping("/personal-info")
    fun submitPersonalInformation(@Validated @RequestBody data: PersonalInformation): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(kycService.submitPersonalInformation(user.id, data))
    }

    @Operation(
        summary = "Submit Address Details",
        description = "Submits or updates the Address details for the currently authenticated user."
    )
    @PostMapping("/address")
    fun submitAddressDetails(@Validated @RequestBody data: UpdateAddressDetailsRequest): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(kycService.submitAddressDetails(user.id, data))
    }

    @Operation(
        summary = "Submit Document Details",
        description = "Submits or updates the Document details for the currently authenticated user."
    )
    @PostMapping("/documents")
    fun submitDocumentDetails(@Validated @RequestBody data: DocumentInformation): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(kycService.submitDocumentDetails(user.id, data))
    }

    @Operation(
        summary = "Approve KYC",
        description = "Approves a KYC record by its ID. Requires ADMIN role."
    )
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    fun approve(@PathVariable id: String): ResponseEntity<KycResponse> = ResponseEntity.ok(
        kycService.approve(
            userService.findUser(id) ?: throw AccountNotFoundException("User not found")
        )
    )

    @Operation(
        summary = "Reject KYC",
        description = "Rejects a KYC record by its id, with an optional reason. Requires ADMIN role."
    )
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun reject(
        @PathVariable id: String,
        @RequestParam(required = false) reason: String?
    ): ResponseEntity<KycResponse> = ResponseEntity.ok(
        kycService.reject(
            userService.findUser(id) ?: throw AccountNotFoundException("User not found"), reason
        )
    )
}
