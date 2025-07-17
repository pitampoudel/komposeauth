package  com.vardansoft.auth.login.data

import com.vardansoft.auth.core.domain.ValidationResult
import com.vardansoft.auth.login.domain.use_cases.ValidateConfirmPassword
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import login.domain.use_cases.ValidatePassword

@Serializable
data class ChangePasswordRequest(
    @SerialName("newPassword")
    val newPassword: String,
    @SerialName("confirmPassword")
    val confirmPassword: String
) {
    init {
        require(ValidatePassword().invoke(newPassword) is ValidationResult.Success)
        require(
            ValidateConfirmPassword().invoke(
                password = newPassword,
                confirmPassword = confirmPassword
            ) is ValidationResult.Success
        )
    }
}
