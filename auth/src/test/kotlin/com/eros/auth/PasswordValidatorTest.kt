package com.eros.auth

import com.eros.auth.validation.PasswordValidator
import com.eros.auth.validation.Errors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertContains

class PasswordValidatorTest {

    // Valid password tests
    @Test
    fun `validate returns success for valid password with all requirements`() {
        val result = PasswordValidator.validate("Password123!")
        assertTrue(result.isValid, "Valid password should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid password should have no errors")
    }

    @Test
    fun `validate returns success for password with minimum length`() {
        val result = PasswordValidator.validate("Pasw0rd!")
        assertTrue(result.isValid, "Password w1ith exactly 8 characters should be valid")
    }

    @Test
    fun `validate returns success for password with multiple special characters`() {
        val result = PasswordValidator.validate("P@ssw0rd!#\$")
        assertTrue(result.isValid, "Password with multiple special characters should be valid")
    }

    @Test
    fun `validate returns success for long password`() {
        val result = PasswordValidator.validate("VeryLongPassword123!WithManyCharacters")
        assertTrue(result.isValid, "Long password meeting all requirements should be valid")
    }

    @Test
    fun `isValid returns true for valid password`() {
        assertTrue(PasswordValidator.isValid("Valid123!"), "isValid should return true for valid password")
    }

    // Null password test
    @Test
    fun `validate returns failure with NULL for null password`() {
        val result = PasswordValidator.validate(null)
        assertFalse(result.isValid, "Null password should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.NULL, "Should contain NULL error")
    }

    @Test
    fun `isValid returns false for null password`() {
        assertFalse(PasswordValidator.isValid(null), "isValid should return false for null password")
    }

    // Empty password test
    @Test
    fun `validate returns failure with EMPTY for empty string`() {
        val result = PasswordValidator.validate("")
        assertFalse(result.isValid, "Empty password should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.EMPTY, "Should contain EMPTY error")
    }

    @Test
    fun `isValid returns false for empty password`() {
        assertFalse(PasswordValidator.isValid(""), "isValid should return false for empty password")
    }

    // Whitespace test
    @Test
    fun `validate returns failure with WHITESPACE for password containing spaces`() {
        val result = PasswordValidator.validate("Pass word123!")
        assertFalse(result.isValid, "Password with spaces should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.WHITESPACE, "Should contain WHITESPACE error")
    }

    @Test
    fun `validate returns failure with WHITESPACE for password with leading space`() {
        val result = PasswordValidator.validate(" Password123!")
        assertFalse(result.isValid, "Password with leading space should fail")
        assertContains(result.errors, Errors.WHITESPACE, "Should contain WHITESPACE error")
    }

    @Test
    fun `validate returns failure with WHITESPACE for password with trailing space`() {
        val result = PasswordValidator.validate("Password123! ")
        assertFalse(result.isValid, "Password with trailing space should fail")
        assertContains(result.errors, Errors.WHITESPACE, "Should contain WHITESPACE error")
    }

    @Test
    fun `validate returns failure with WHITESPACE for password with tab character`() {
        val result = PasswordValidator.validate("Password\t123!")
        assertFalse(result.isValid, "Password with tab should fail")
        assertContains(result.errors, Errors.WHITESPACE, "Should contain WHITESPACE error")
    }

    // Length test
    @Test
    fun `validate returns failure with LENGTH for password shorter than minimum`() {
        val result = PasswordValidator.validate("Pass1!")
        assertFalse(result.isValid, "Password shorter than 8 characters should fail")
        assertContains(result.errors, Errors.LENGTH, "Should contain LENGTH error")
    }

    @Test
    fun `validate returns failure with LENGTH for 7 character password`() {
        val result = PasswordValidator.validate("Pass1!A")
        assertFalse(result.isValid, "7 character password should fail")
        assertContains(result.errors, Errors.LENGTH, "Should contain LENGTH error")
    }

    // Uppercase missing test
    @Test
    fun `validate returns failure with UPPER_MISSING for password without uppercase`() {
        val result = PasswordValidator.validate("password123!")
        assertFalse(result.isValid, "Password without uppercase should fail")
        assertContains(result.errors, Errors.UPPER_MISSING, "Should contain UPPER_MISSING error")
    }

    @Test
    fun `validate returns failure with UPPER_MISSING for all lowercase password`() {
        val result = PasswordValidator.validate("alllowercase123!")
        assertFalse(result.isValid, "All lowercase password should fail")
        assertContains(result.errors, Errors.UPPER_MISSING, "Should contain UPPER_MISSING error")
    }

    // Lowercase missing test
    @Test
    fun `validate returns failure with LOWER_MISSING for password without lowercase`() {
        val result = PasswordValidator.validate("PASSWORD123!")
        assertFalse(result.isValid, "Password without lowercase should fail")
        assertContains(result.errors, Errors.LOWER_MISSING, "Should contain LOWER_MISSING error")
    }

    @Test
    fun `validate returns failure with LOWER_MISSING for all uppercase password`() {
        val result = PasswordValidator.validate("ALLUPPERCASE123!")
        assertFalse(result.isValid, "All uppercase password should fail")
        assertContains(result.errors, Errors.LOWER_MISSING, "Should contain LOWER_MISSING error")
    }

    // Digit missing test
    @Test
    fun `validate returns failure with DIGIT_MISSING for password without digits`() {
        val result = PasswordValidator.validate("Password!")
        assertFalse(result.isValid, "Password without digits should fail")
        assertContains(result.errors, Errors.DIGIT_MISSING, "Should contain DIGIT_MISSING error")
    }

    @Test
    fun `validate returns failure with DIGIT_MISSING for alphabetic only password`() {
        val result = PasswordValidator.validate("OnlyLetters!")
        assertFalse(result.isValid, "Alphabetic only password should fail")
        assertContains(result.errors, Errors.DIGIT_MISSING, "Should contain DIGIT_MISSING error")
    }

    // Special character missing test
    @Test
    fun `validate returns failure with SPECIAL_MISSING for password without special characters`() {
        val result = PasswordValidator.validate("Password123")
        assertFalse(result.isValid, "Password without special characters should fail")
        assertContains(result.errors, Errors.SPECIAL_MISSING, "Should contain SPECIAL_MISSING error")
    }

    @Test
    fun `validate returns failure with SPECIAL_MISSING for alphanumeric only password`() {
        val result = PasswordValidator.validate("Alphanumeric123")
        assertFalse(result.isValid, "Alphanumeric only password should fail")
        assertContains(result.errors, Errors.SPECIAL_MISSING, "Should contain SPECIAL_MISSING error")
    }

    // Multiple errors test
    @Test
    fun `validate returns multiple errors for password with multiple issues`() {
        val result = PasswordValidator.validate("pass")
        assertFalse(result.isValid, "Invalid password should fail")
        assertTrue(result.errors.size > 1, "Should have multiple errors")
        assertContains(result.errors, Errors.LENGTH, "Should contain LENGTH error")
        assertContains(result.errors, Errors.UPPER_MISSING, "Should contain UPPER_MISSING error")
        assertContains(result.errors, Errors.DIGIT_MISSING, "Should contain DIGIT_MISSING error")
        assertContains(result.errors, Errors.SPECIAL_MISSING, "Should contain SPECIAL_MISSING error")
    }

    @Test
    fun `validate returns all errors for completely invalid password`() {
        val result = PasswordValidator.validate("abc")
        assertFalse(result.isValid, "Invalid password should fail")
        assertEquals(4, result.errors.size, "Should have 4 errors")
        assertContains(result.errors, Errors.LENGTH, "Should contain LENGTH error")
        assertContains(result.errors, Errors.UPPER_MISSING, "Should contain UPPER_MISSING error")
        assertContains(result.errors, Errors.DIGIT_MISSING, "Should contain DIGIT_MISSING error")
        assertContains(result.errors, Errors.SPECIAL_MISSING, "Should contain SPECIAL_MISSING error")
    }

    @Test
    fun `validate returns LENGTH and SPECIAL_MISSING for short password without special chars`() {
        val result = PasswordValidator.validate("Pass123")
        assertFalse(result.isValid, "Invalid password should fail")
        assertContains(result.errors, Errors.LENGTH, "Should contain LENGTH error")
        assertContains(result.errors, Errors.SPECIAL_MISSING, "Should contain SPECIAL_MISSING error")
    }

    // Edge case tests with various special characters
    @Test
    fun `validate accepts password with exclamation mark`() {
        val result = PasswordValidator.validate("Password123!")
        assertTrue(result.isValid, "Password with ! should be valid")
    }

    @Test
    fun `validate accepts password with at symbol`() {
        val result = PasswordValidator.validate("P@ssword123")
        assertTrue(result.isValid, "Password with @ should be valid")
    }

    @Test
    fun `validate accepts password with hash symbol`() {
        val result = PasswordValidator.validate("Password#123")
        assertTrue(result.isValid, "Password with # should be valid")
    }

    @Test
    fun `validate accepts password with dollar sign`() {
        val result = PasswordValidator.validate("Pa\$sword123")
        assertTrue(result.isValid, "Password with $ should be valid")
    }

    @Test
    fun `validate accepts password with brackets`() {
        val result = PasswordValidator.validate("Pass[word]123")
        assertTrue(result.isValid, "Password with brackets should be valid")
    }

    // Helper method tests
    @Test
    fun `getMinLength returns correct minimum length`() {
        assertEquals(8, PasswordValidator.getMinLength(), "Minimum length should be 8")
    }

    @Test
    fun `getRequirements returns all requirements`() {
        val requirements = PasswordValidator.getRequirements()
        assertEquals(5, requirements.size, "Should have 5 requirements")
        assertTrue(requirements.any { it.contains("8 characters") }, "Should mention length requirement")
        assertTrue(requirements.any { it.contains("uppercase") }, "Should mention uppercase requirement")
        assertTrue(requirements.any { it.contains("lowercase") }, "Should mention lowercase requirement")
        assertTrue(requirements.any { it.contains("digit") }, "Should mention digit requirement")
        assertTrue(requirements.any { it.contains("special character") }, "Should mention special character requirement")
    }
}
