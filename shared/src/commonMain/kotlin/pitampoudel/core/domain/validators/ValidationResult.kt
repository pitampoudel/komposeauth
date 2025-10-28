package pitampoudel.core.domain.validators

sealed class ValidationResult {
    data object Success : ValidationResult()
    open class Error(val message: GeneralValidationError) : ValidationResult()

    fun isSuccess() = this is Success
    fun error(): GeneralValidationError? {
        if (this is Error) {
            return this.message
        }
        return null
    }
}