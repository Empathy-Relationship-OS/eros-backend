package com.eros.auth.validation

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException


/**
 * Age validator.
 *
 * Provided a birthdate in ISO-8601 format: YYYY-MM-DD
 * Ensure the age is at least 18.
 *
 * Functions:
 *
 * `validate` - Returns a [ValidationResult] upon successful or unsuccessful validation.
 *
 * `isValid` - Returns `true` if a valid age otherwise `false`.
 */
object AgeValidator {
    /**
     * Validates that the user is at least 18 years old.
     * Birthdate should be provided in ISO-8601 format: YYYY-MM-DD
     *
     * @returns [ValidationResult] success or failure with error if applicable.
     */
    fun validate(birthDate : LocalDate): ValidationResult {
        try {
            val age = Period.between(birthDate, LocalDate.now()).years
            if (age < 18) {
                return ValidationResult.failure(Errors.UNDERAGE)
            }
        } catch (_: DateTimeParseException) {
            // TODO maybe we log e, for greater visibility, might not be needed
            return ValidationResult.failure(Errors.DATE_FORMATTING)
        }
        return ValidationResult.success()
    }

    fun isValid(birthDate: LocalDate) : Boolean{
        return validate(birthDate).isValid
    }
}