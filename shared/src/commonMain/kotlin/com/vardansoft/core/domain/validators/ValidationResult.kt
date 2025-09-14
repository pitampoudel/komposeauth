package com.vardansoft.core.domain.validators

sealed class ValidationResult {
    data object Success : ValidationResult()
    open class Error(val message: String) : ValidationResult()
    fun errorMessage() = if (this is Error) this.message else null
    fun isSuccess() = this is Success
}