package com.vardansoft.core.domain.validators

sealed class ValidationResult {
    data object Success : ValidationResult()
    open class Error(val message: AuthXValidationError) : ValidationResult()

    fun isSuccess() = this is Success
    fun error(): AuthXValidationError? {
        if (this is Error) {
            return this.message
        }
        return null
    }
}