package com.vardansoft.authx.data

import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.authx.domain.use_cases.ValidateConfirmPassword
import com.vardansoft.authx.domain.use_cases.ValidatePassword
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
