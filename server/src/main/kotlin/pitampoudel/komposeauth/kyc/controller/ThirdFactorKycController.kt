package pitampoudel.komposeauth.kyc.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.SlackNotifier
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService

@RestController

class ThirdFactorKycController(
    val userService: UserService,
    private val kycService: KycService,
    private val emailService: EmailService,
    private val slackNotifier: SlackNotifier
) {
    @PostMapping("/$THIRD_FACTOR_KYC")
    fun submit(@Validated @RequestBody data: ThirdFactorModel): ResponseEntity<*> {


        return ResponseEntity.ok("received successfully")
    }


}