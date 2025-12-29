package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.domain.validators.ValidationResult
import pitampoudel.komposeauth.user.domain.validators.ValidateConfirmPassword
import pitampoudel.komposeauth.user.domain.validators.ValidatePassword

@Serializable
data class ChangePasswordRequest(
    @SerialName("newPassword")
    val newPassword: String,
    @SerialName("confirmPassword")
    val confirmPassword: String
) {
    init {
        require(ValidatePassword(newPassword) is ValidationResult.Success)
        require(
            ValidateConfirmPassword(
                password = newPassword,
                confirmPassword = confirmPassword
            ) is ValidationResult.Success
        )
    }
}