package pitampoudel.komposeauth.kyc.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.data.ApiEndpoints
import pitampoudel.komposeauth.core.data.ApiEndpoints.KYC_ADDRESS
import pitampoudel.komposeauth.core.data.ApiEndpoints.KYC_DOCUMENTS
import pitampoudel.komposeauth.core.data.ApiEndpoints.KYC_PENDING
import pitampoudel.komposeauth.core.data.ApiEndpoints.KYC_PERSONAL_INFO
import pitampoudel.komposeauth.core.utils.findCurrentBaseUrl
import pitampoudel.komposeauth.kyc.data.DocumentInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService
import javax.security.auth.login.AccountNotFoundException

@RestController
class KycController(
    val userService: UserService,
    private val userContextService: UserContextService,
    private val kycService: KycService,
    private val emailService: EmailService,
) {
    @Operation(
        summary = "Get KYC information for current user",
        description = "Retrieves the Know Your Customer (KYC) information for the currently authenticated user."
    )
    @GetMapping("/${ApiEndpoints.KYC}")
    fun getMine(): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        val kyc = kycService.find(user.id)
        return if (kyc != null) ResponseEntity.ok(kyc) else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Get all pending KYC submissions",
        description = "Retrieves all KYC submissions with a 'PENDING' status. Requires ADMIN role."
    )
    @GetMapping("/$KYC_PENDING")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPending(): ResponseEntity<List<KycResponse>> = ResponseEntity.ok(kycService.getPending())

    @Operation(
        summary = "Submit Personal Information",
        description = "Submits or updates the Personal information for the currently authenticated user."
    )
    @PostMapping("/$KYC_PERSONAL_INFO")
    fun submitPersonalInformation(@Validated @RequestBody data: PersonalInformation): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(kycService.submitPersonalInformation(user.id, data))
    }

    @Operation(
        summary = "Submit Address Details",
        description = "Submits or updates the Address details for the currently authenticated user."
    )
    @PostMapping("/$KYC_ADDRESS")
    fun submitAddressDetails(@Validated @RequestBody data: UpdateAddressDetailsRequest): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(kycService.submitAddressDetails(user.id, data))
    }

    @Operation(
        summary = "Submit Document Details",
        description = "Submits or updates the Document details for the currently authenticated user."
    )
    @PostMapping("/$KYC_DOCUMENTS")
    fun submitDocumentDetails(
        @Validated @RequestBody data: DocumentInformation,
        request: HttpServletRequest
    ): ResponseEntity<KycResponse> {
        val user = userContextService.getUserFromAuthentication()
        val result = kycService.submitDocumentDetails(user.id, data)
        user.email?.let {
            emailService.sendHtmlMail(
                baseUrl = findCurrentBaseUrl(request),
                to = it,
                subject = "KYC Documents Received",
                template = "email/generic.html",
                model = mapOf(
                    "title" to "KYC Submission Received",
                    "name" to user.fullName,
                    "message" to "Thank you for submitting your KYC documents. We are now reviewing your information and will notify you once the process is complete. This usually takes 1-2 business days."
                )
            )
        }
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Approve KYC",
        description = "Approves a KYC record by its ID. Requires ADMIN role."
    )
    @PostMapping("/kyc/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    fun approve(
        @PathVariable id: String,
        request: HttpServletRequest
    ): ResponseEntity<KycResponse> = ResponseEntity.ok(
        kycService.approve(
            baseUrl = findCurrentBaseUrl(request),
            userService.findUser(id) ?: throw AccountNotFoundException("User not found")
        )
    )

    @Operation(
        summary = "Reject KYC",
        description = "Rejects a KYC record by its id, with an optional reason. Requires ADMIN role."
    )
    @PostMapping("/kyc/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun reject(
        httpServletRequest: HttpServletRequest,
        @PathVariable id: String,
        @RequestParam(required = false) reason: String?
    ): ResponseEntity<KycResponse> = ResponseEntity.ok(
        kycService.reject(
            baseUrl = findCurrentBaseUrl(httpServletRequest),
            user = userService.findUser(id) ?: throw AccountNotFoundException("User not found"),
            reason = reason
        )
    )
}