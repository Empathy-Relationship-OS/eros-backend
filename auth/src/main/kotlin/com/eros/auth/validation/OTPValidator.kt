package com.eros.auth.validation

/**
 * Validates the OTP format.
 *
 * Functions:
 *
 * `validate` - Returns a [ValidationResult] upon successful or unsuccessful validation.
 *
 * `isValid` - Returns `true` if a valid OTP format otherwise `false`.
*/
object OTPValidator {

    const val DIGITS_LENGTH = 6

    /**
    * Validates the OTP format.
    *
    * Requirements.
    * Must be all digits and [DIGITS_LENGTH] digits.
    *
    * @param otp otp to be checked for formatting.
    * @returns [ValidationResult] with success or failure and relevant errors if applicable.
    */
    fun validate(otp : String) : ValidationResult {
        if (otp.isBlank()) {
            return ValidationResult.failure(Errors.OTP_BLANK)
        }

        if (!otp.all { it.isDigit() }) {
            return ValidationResult.failure(Errors.OTP_NON_DIGITS)
        }

        if (otp.length != DIGITS_LENGTH) {
            return ValidationResult.failure(Errors.OTP_SIZE)
        }
        return ValidationResult.success()
    }

    /**
     * Validates the OTP format.
     *
     * Requirements.
     * Must be all digits and [DIGITS_LENGTH] digits.
     *
     * @param otp otp to be checked for formatting.
     * @returns `true` if valid OTP format otherwise `false`
     */
    fun isValid(otp : String) : Boolean{
        return validate(otp).isValid
    }

}