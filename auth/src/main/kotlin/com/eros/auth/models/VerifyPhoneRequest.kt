package com.eros.auth.models

import com.eros.auth.validation.Errors
import com.eros.auth.validation.OTPValidator
import com.eros.auth.validation.PhoneValidator
import com.eros.auth.validation.ValidationResult
import kotlinx.serialization.Serializable


/**
 * Request model for phone verification.
 *
 * @property phone User's phone number to verify
 * @property otp One-time password sent to the phone
 */
@Serializable
data class VerifyPhoneRequest(
    val phone: String,
    val otp: String
) {
    /**
     * Validates the phone verification request.
     *
     * Validation rules:
     * - Phone must be valid format (checked by PhoneValidator)
     * - OTP must not be blank
     * - OTP must be numeric
     * - OTP must be appropriate length.
     *
     * @return [ValidationResult] containing success or failure and errors.
     */
    fun validate() : ValidationResult{

        val errors = mutableListOf<Errors>()

        val phoneResult = PhoneValidator.validate(phone)
        errors.addAll(phoneResult.errors)

        val otpResult = OTPValidator.validate(otp)
        errors.addAll(otpResult.errors)

        if (errors.isEmpty()){
            return ValidationResult.success()
        }
        else{
            return ValidationResult.failure(errors);
        }
    }


    /**
     * Validates the phone verification request.
     *
     * Validation rules:
     * - Phone must be valid format (checked by PhoneValidator)
     * - OTP must not be blank
     * - OTP must be numeric
     * - OTP must be appropriate length (typically 4-6 digits)
     *
     * @return `true` if valid otherwise `false`
     */
    fun isValid() : Boolean{
        return validate().isValid
    }
}