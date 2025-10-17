package com.vardansoft.komposeauth.core.service.sms

import com.vardansoft.komposeauth.AppProperties
import com.vardansoft.komposeauth.user.entity.PhoneOtp
import com.vardansoft.komposeauth.user.repository.PhoneOtpRepository
import kotlin.random.Random

class SamayePhoneNumberVerificationService(
    private val phoneOtpRepository: PhoneOtpRepository,
    private val smsService: SmsService,
    private val appProperties: AppProperties
) : PhoneNumberVerificationService {

    override fun initiate(phoneNumber: String): Boolean {
        val otp = generateOtp()
        phoneOtpRepository.save(
            PhoneOtp(
                phoneNumber = phoneNumber,
                otp = otp
            )
        )
        return smsService.sendSms(phoneNumber, "Your OTP for ${appProperties.name} is $otp")
    }

    override fun verify(phoneNumber: String, code: String): Boolean {
        val otpRecords = phoneOtpRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
        if (otpRecords.isEmpty()) {
            return false
        }
        val latestOtp = otpRecords.first()
        if (latestOtp.otp == code && !latestOtp.isExpired()) {
            phoneOtpRepository.deleteByPhoneNumber(phoneNumber)
            return true
        }
        return false
    }

    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
