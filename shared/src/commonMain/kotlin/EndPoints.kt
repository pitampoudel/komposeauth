package com.vardansoft.auth

object EndPoints {
    fun apiUrl(endPoint: String): String {
        return "${VardanSoftAuth.AUTH_BASE_URL}/$endPoint"
    }

    const val TOKEN = "oauth2/token"
    const val USER_INFO = "userinfo"
    const val UPDATE_PHONE_NUMBER = "phone-number/update"
    const val VERIFY_PHONE_NUMBER = "phone-number/verify"
}