package com.eros.auth.validation

/**
 * Email validator.
 *
 * Following the RFC 5322 format.
 * Local: "a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-"
 * Required '@' symbol
 * Domain: "a-zA-Z0-9.-"
 *
 * Functions:
 *
 * `validate` - Returns a [ValidationResult] upon successful or unsuccessful validation.
 *
 * `isValid` - Returns `true` if a valid email otherwise `false`.
 */
object EmailValidator {
    private val emailRegex = Regex("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")

    /**
     * Creates a `ValidationResult` object containing boolean of success and list of [Errors].
     *
     * @param email : String of the email that is being validated.
     * @return `ValidationResult` containing information of successful / failure.
     */
    fun validate(email: String?): ValidationResult {
        if (email == null) {
            return ValidationResult.failure(Errors.EMAIL_NULL)
        }

        if (email.isEmpty()) {
            return ValidationResult.failure(Errors.EMAIL_EMPTY)
        }

        if (emailRegex.matches(email)) {
            return ValidationResult.success()
        } else {
            return ValidationResult.failure(Errors.EMAIL_INVALID)
        }
    }

    /**
     * Function to verify if an email is valid or not.
     *
     * @param email : String of the email that is being validated.
     * @return `true` if valid email otherwise `false`.
     */
    fun isValid(email: String?) : Boolean{
        return validate(email).isValid
    }

}