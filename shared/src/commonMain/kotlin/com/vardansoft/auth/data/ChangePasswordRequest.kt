package com.vardansoft.auth.data

import com.vardansoft.auth.domain.ValidationResult
import com.vardansoft.auth.domain.use_cases.ValidateConfirmPassword
import com.vardansoft.auth.domain.use_cases.ValidatePassword
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
