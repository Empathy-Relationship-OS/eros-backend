package com.eros.auth.validation

/**
 * Phone Number Validator
 *
 * Provides phone number validation according to E.164 international format.
 *
 * E.164 Format:
 * - Starts with a '+' sign
 * - Followed by country code (1-3 digits)
 * - Followed by subscriber number
 * - Total length: 1-15 digits (excluding the '+')
 * - No spaces, hyphens, or other formatting characters
 *
 * Examples of valid E.164 numbers:
 * - +14155552671 (US)
 * - +442071838750 (UK)
 * - +861012345678 (China)
 */
object PhoneValidator {

    const val MIN_DIGITS = 1
    const val MAX_DIGITS = 15

    /**
     * Validates a phone number against E.164 international format.
     *
     * @param phoneNumber The phone number string to validate (nullable)
     * @return ValidationResult containing validation status and any errors
     */
    fun validate(phoneNumber: String?): ValidationResult {

        if (phoneNumber == null) return ValidationResult.failure(Errors.PHONE_NULL)
        if (phoneNumber.isEmpty()) return ValidationResult.failure(Errors.PHONE_EMPTY)
        if (!phoneNumber.startsWith('+')) return ValidationResult.failure(Errors.PHONE_PLUS)

        val digits = phoneNumber.substring(1)

        if (digits.isEmpty()) return ValidationResult.failure(Errors.PHONE_EMPTY)
        if (digits.any { !it.isDigit() }) return ValidationResult.failure(Errors.PHONE_DIGITS)
        if (digits[0] == '0') return ValidationResult.failure(Errors.PHONE_ZERO)

        val errors = mutableListOf<Errors>()

        if (digits.length < MIN_DIGITS) errors.add(Errors.PHONE_SHORT)
        if (digits.length > MAX_DIGITS) errors.add(Errors.PHONE_LONG)

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    /**
     * Checks if a phone number is valid (convenience method).
     *
     * @param phoneNumber The phone number string to validate
     * @return True if the phone number is valid, false otherwise
     */
    fun isValid(phoneNumber: String?): Boolean = validate(phoneNumber).isValid

    /**
     * Returns information about the E.164 format.
     *
     * @return Description of E.164 format requirements
     */
    fun getFormatInfo(): String = """
        E.164 International Phone Number Format:
        - Must start with '+' sign
        - Followed by country code and subscriber number
        - Total of $MIN_DIGITS-$MAX_DIGITS digits (excluding '+')
        - Cannot start with 0 after the '+'
        - No spaces, hyphens, or other formatting characters
        Examples: +14155552671, +442071838750, +861012345678
    """.trimIndent()

    /**
     * Attempts to normalize a phone number by removing common formatting.
     *
     * This method removes spaces, hyphens, parentheses, and dots. It does NOT
     * add country codes or validate the result.
     *
     * @param phoneNumber The phone number string to normalize
     * @return Normalized phone number or null if input is null
     */
    fun normalize(phoneNumber: String?): String? {
        if (phoneNumber == null) return null

        return phoneNumber
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")
    }

    /**
     * Returns the minimum number of digits allowed.
     *
     * @return Minimum digit count
     */
    fun getMinDigits(): Int = MIN_DIGITS

    /**
     * Returns the maximum number of digits allowed.
     *
     * @return Maximum digit count
     */
    fun getMaxDigits(): Int = MAX_DIGITS
}