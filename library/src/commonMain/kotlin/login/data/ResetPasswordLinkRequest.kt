package login.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.vardansoft.auth.core.domain.ValidationResult
import com.vardansoft.auth.login.domain.use_cases.ValidateEmail

@Serializable
data class ResetPasswordLinkRequest(
    @SerialName("email")
    val email: String
) {
    init {
        require(ValidateEmail().invoke(email) is ValidationResult.Success)
    }
}
