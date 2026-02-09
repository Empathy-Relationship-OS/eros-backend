package com.eros.auth.validation

/**
 * Password Validator
 *
 * Provides comprehensive password validation according to security best practices.
 *
 * Validation Rules:
 * - Minimum length: 8 characters
 * - Must contain at least one uppercase letter (A-Z)
 * - Must contain at least one lowercase letter (a-z)
 * - Must contain at least one digit (0-9)
 * - Must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
 */
object PasswordValidator {

    const val MIN_LENGTH = 8
    private const val SPECIAL_CHARS = "!@#\$%^&*()_+\\-=[]{}|;:,.<>?"

    /**
     * Validates a password against all security requirements.
     *
     * @param password The password string to validate (nullable)
     * @return ValidationResult containing validation status and any error messages.
     *
     */
    fun validate(password: String?): ValidationResult {

        if (password == null) return ValidationResult.failure(Errors.NULL)
        if (password.isEmpty()) return ValidationResult.failure(Errors.EMPTY)
        if (password.any { it.isWhitespace() }) return ValidationResult.failure(Errors.WHITESPACE)

        val errors = mutableListOf<Errors>()
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSpecial = false

        for (char in password) {
            when {
                char.isUpperCase() -> hasUpper = true
                char.isLowerCase() -> hasLower = true
                char.isDigit() -> hasDigit = true
                SPECIAL_CHARS.contains(char) -> hasSpecial = true
            }
        }

        if (password.length < MIN_LENGTH) errors.add(Errors.LENGTH)
        if (!hasUpper) errors.add(Errors.UPPER_MISSING)
        if (!hasLower) errors.add(Errors.LOWER_MISSING)
        if (!hasDigit) errors.add(Errors.DIGIT_MISSING)
        if (!hasSpecial) errors.add(Errors.SPECIAL_MISSING)

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    /**
     * Checks if a password is valid (convenience method).
     *
     * @param password The password string to validate
     * @return True if the password is valid, false otherwise
     */
    fun isValid(password: String?): Boolean = validate(password).isValid

    /**
     * Returns a list of all password requirements.
     *
     * @return List of requirement descriptions
     */
    fun getRequirements(): List<String> = listOf(
        "At least $MIN_LENGTH characters long",
        "At least one uppercase letter (A-Z)",
        "At least one lowercase letter (a-z)",
        "At least one digit (0-9)",
        "At least one special character $SPECIAL_CHARS"
    )

    /**
     * Returns the minimum password length requirement.
     *
     * @return Minimum password length
     */
    fun getMinLength(): Int = MIN_LENGTH
}


fun main() {

    val pw = "awd4drgGH^wa"
    val pw2 = "  "

    val res = PasswordValidator.validate(pw2)

    for (error in res.errors){
        println(error.message)
    }

    println(res.isValid)
    println(res.errors)

    println(PasswordValidator.isValid(pw))

    println(PasswordValidator.getRequirements())

}