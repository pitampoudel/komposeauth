package com.vardansoft.komposeauth.data

import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.komposeauth.domain.use_cases.ValidateConfirmPassword
import com.vardansoft.komposeauth.domain.use_cases.ValidatePassword
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
        require(ValidatePassword(newPassword) is ValidationResult.Success)
        require(
            ValidateConfirmPassword(
                password = newPassword,
                confirmPassword = confirmPassword
            ) is ValidationResult.Success
        )
    }
}
