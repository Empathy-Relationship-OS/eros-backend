package com.eros.auth.models

import com.eros.auth.validation.EmailValidator
import com.eros.auth.validation.Errors
import com.eros.auth.validation.PasswordValidator
import com.eros.auth.validation.ValidationResult
import kotlinx.serialization.Serializable


/**
 * Request model for user login.
 *
 * @property email User's email address.
 * @property password User's password.
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
) {

    /**
     * Validates the login request.
     *
     * Validation rules:
     * - Email must be valid format (checked by [EmailValidator])
     * - Password must meet strength requirements (checked by [PasswordValidator])
     *
     * @return [ValidationResult] containing success or failure and errors.
     */
    fun validate() : ValidationResult{
        val errors = mutableListOf<Errors>()

        val emailResult = EmailValidator.validate(email)
        errors.addAll(emailResult.errors)

        val passwordResult = PasswordValidator.validate(password)
        errors.addAll(passwordResult.errors)

        return if (errors.isEmpty()) {
            ValidationResult.success()
        }else{
            ValidationResult.failure(errors)
        }
    }

    /**
     * Validates the login request.
     *
     * Validation rules:
     * - Email must be valid format (checked by [EmailValidator])
     * - Password must meet strength requirements (checked by [PasswordValidator])
     *
     * @return `true` if valid other `false`
     */
    fun isValid() : Boolean{
        return validate().isValid
    }
}