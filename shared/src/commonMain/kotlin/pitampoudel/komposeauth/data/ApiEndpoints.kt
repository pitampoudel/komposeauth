package pitampoudel.komposeauth.data


object ApiEndpoints {
    const val JWKS = "oauth2/jwks"
    const val TOKEN = "token"
    const val USERS = "users"
    const val USERS_IN_BULK = "users/bulk"
    const val ME = "me"
    const val DEACTIVATE = "deactivate"
    const val UPDATE_PROFILE = "update-profile"
    const val UPDATE_PHONE_NUMBER = "phone-number/update"
    const val VERIFY_PHONE_NUMBER = "phone-number/verify"
    const val LOGIN_OPTIONS = "login-options"
    const val KYC = "kyc"
    const val KYC_PENDING = "$KYC/pending"
    const val KYC_PERSONAL_INFO = "$KYC/personal-info"
    const val KYC_DOCUMENTS = "$KYC/documents"
    const val KYC_ADDRESS = "$KYC/address"
    const val OAUTH2_CLIENTS = "oauth2/clients"
    const val VERIFY_EMAIL = "verify-email"
    const val RESET_PASSWORD = "reset-password"


}
