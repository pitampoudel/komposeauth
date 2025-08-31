package com.vardansoft.authx.data

/**
 * Centralized API endpoint path segments shared between client and server.
 */
object ApiEndpoints {
    const val TOKEN = "oauth2/token"
    const val USER_INFO = "userinfo"
    const val UPDATE_PHONE_NUMBER = "phone-number/update"
    const val VERIFY_PHONE_NUMBER = "phone-number/verify"
    const val CONFIG = "config"
    const val KYC = "kyc"
}
