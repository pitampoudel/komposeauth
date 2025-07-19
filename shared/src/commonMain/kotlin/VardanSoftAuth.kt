package com.vardansoft.auth

object VardanSoftAuth {

    val GOOGLE_ID = BuildKonfig.AUTH_GOOGLE_ID
    val URL = BuildKonfig.AUTH_URL

    object EndPoints {
        fun apiUrl(endPoint: String): String {
            return "${URL}/$endPoint"
        }

        const val TOKEN = "oauth2/token"
        const val USER_INFO = "userinfo"
        const val UPDATE_PHONE_NUMBER = "phone-number/update"
        const val VERIFY_PHONE_NUMBER = "phone-number/verify"


    }

}