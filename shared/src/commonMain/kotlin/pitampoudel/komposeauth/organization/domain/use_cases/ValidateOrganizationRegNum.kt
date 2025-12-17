package pitampoudel.komposeauth.organization.domain.use_cases

import pitampoudel.core.domain.validators.ValidationResult

object ValidateOrganizationRegNum {
    operator fun invoke(value: String): ValidationResult {
        return ValidationResult.Success
    }
}
