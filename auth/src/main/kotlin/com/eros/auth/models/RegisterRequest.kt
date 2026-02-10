package com.eros.auth.models

import com.eros.auth.validation.AgeValidator
import com.eros.auth.validation.EmailValidator
import com.eros.auth.validation.Errors
import com.eros.auth.validation.PasswordValidator
import com.eros.auth.validation.PhoneValidator
import com.eros.auth.validation.ValidationResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

import kotlinx.serialization.Serializable


/**
 * Request model for user registration.
 *
 * @property email User's email address
 * @property password User's password
 * @property phone User's phone number
 * @property name User's full name
 * @property birthDate User's date of birth (ISO-8601 format: YYYY-MM-DD)
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val phone: String,
    val name: String,
    val birthDate: LocalDate,
) {
    /**
     * Validates the registration request.
     *
     * Validation rules:
     * - Email must be valid format (checked by [EmailValidator])
     * - Password must meet strength requirements (checked by [PasswordValidator])
     * - Phone must be valid format (checked by [PhoneValidator])
     * - User must be 18 years or older (checked by [AgeValidator])
     * - Name must not be blank
     *
     * @return [ValidationResult] containing success or failure and errors.
     */
    fun validate() : ValidationResult {

        val errors = mutableListOf<Errors>()

        if (name.isBlank()) {
            errors.add(Errors.BLANK_NAME)
        }

        val emailResult = EmailValidator.validate(email)
        errors.addAll(emailResult.errors)

        val passwordResult = PasswordValidator.validate(password)
        errors.addAll(passwordResult.errors)

        val phoneResult = PhoneValidator.validate(phone)
        errors.addAll(phoneResult.errors)

        val ageResult = AgeValidator.validate(birthDate.toJavaLocalDate())
        errors.addAll(ageResult.errors)

        return if (errors.isEmpty()) {
            ValidationResult.success()
        }else{
            ValidationResult.failure(errors)
        }
    }


    /**
     * Validates the registration request.
     *
     * Validation rules:
     * - Email must be valid format (checked by [EmailValidator])
     * - Password must meet strength requirements (checked by [PasswordValidator])
     * - Phone must be valid format (checked by [PhoneValidator])
     * - User must be 18 years or older (checked by [AgeValidator])
     * - Name must not be blank
     *
     * @return `true` if valid otherwise `false`
     */
    fun isValid() : Boolean{
        return validate().isValid
    }
}