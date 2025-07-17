package com.vardansoft.auth.user.service

import com.vardansoft.auth.core.service.SmsService
import com.vardansoft.auth.user.entity.PhoneOtp
import com.vardansoft.auth.user.entity.User
import com.vardansoft.auth.user.repository.PhoneOtpRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class PhoneOtpService(
    private val phoneOtpRepository: PhoneOtpRepository,
    private val smsService: SmsService
) {

    fun generateAndSendOtp(user: User, phoneNumber: String): Boolean {
        val otp = generateOtp()
        phoneOtpRepository.save(
            PhoneOtp(
                userId = user.id,
                phoneNumber = phoneNumber,
                otp = otp
            )
        )
        return smsService.sendOtp(user, phoneNumber, otp)
    }

    fun verifyOtp(userId: ObjectId, otp: String): PhoneOtp? {
        val otpRecords = phoneOtpRepository.findByUserIdOrderByCreatedAtDesc(userId)
        if (otpRecords.isEmpty()) {
            return null
        }
        val latestOtp = otpRecords.first()
        // Check if OTP matches and is not expired
        if (latestOtp.otp == otp && !latestOtp.isExpired()) {
            // Clean up used OTP
            phoneOtpRepository.deleteByUserId(userId)
            return latestOtp
        }
        return null
    }

    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
