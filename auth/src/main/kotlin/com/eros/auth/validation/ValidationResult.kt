package com.eros.auth.validation

/**
 * Represents the result of a validation operation.
 *
 * @property isValid True if the validation passed, false otherwise
 * @property errors A list of [Errors] encountered during validation; empty if the password is valid.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<Errors> = emptyList()
) {
    companion object {
        /**
         * Creates a successful validation result.
         */
        fun success(): ValidationResult = ValidationResult(true, emptyList())

        /**
         * Creates a failed validation result with error messages.
         *
         * @param errors List of error messages
         */
        fun failure(errors: List<Errors>): ValidationResult =
            ValidationResult(false, errors)

        /**
         * Creates a failed validation result with a single error message.
         *
         * @param error Single error message
         */
        fun failure(error: Errors): ValidationResult =
            ValidationResult(false, listOf(error))
    }
}