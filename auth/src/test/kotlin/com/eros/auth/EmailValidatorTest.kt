package com.eros.auth

import com.eros.auth.validation.EmailValidator
import com.eros.auth.validation.Errors
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertContains

class EmailValidatorTest {

    // Valid email tests
    @Test
    fun `validate returns success for valid simple email`() {
        val result = EmailValidator.validate("user@example.com")
        assertTrue(result.isValid, "Valid email should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid email should have no errors")
    }

    @Test
    fun `validate returns success for email with plus sign`() {
        val result = EmailValidator.validate("user+tag@example.com")
        assertTrue(result.isValid, "Email with plus sign should be valid")
    }

    @Test
    fun `validate returns success for email with dots in local part`() {
        val result = EmailValidator.validate("user.name@example.com")
        assertTrue(result.isValid, "Email with dots in local part should be valid")
    }

    @Test
    fun `validate returns success for email with special characters`() {
        val result = EmailValidator.validate("user_name#test@example.com")
        assertTrue(result.isValid, "Email with allowed special characters should be valid")
    }

    @Test
    fun `validate returns success for short email`() {
        val result = EmailValidator.validate("a@e.uk")
        assertTrue(result.isValid, "Short valid email should pass")
    }

    @Test
    fun `validate returns success for email with all allowed local characters`() {
        val result = EmailValidator.validate("user!#\$%&'*+/=?`{|}~^.-_@example.com")
        assertTrue(result.isValid, "Email with all allowed RFC 5322 characters should be valid")
    }

    @Test
    fun `validate returns success for email with subdomain`() {
        val result = EmailValidator.validate("user@mail.example.com")
        assertTrue(result.isValid, "Email with subdomain should be valid")
    }

    @Test
    fun `isValid returns true for valid email`() {
        assertTrue(EmailValidator.isValid("user@example.com"), "isValid should return true for valid email")
    }

    // Null email test
    @Test
    fun `validate returns failure with EMAIL_NULL for null email`() {
        val result = EmailValidator.validate(null)
        assertFalse(result.isValid, "Null email should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.EMAIL_NULL, "Should contain EMAIL_NULL error")
    }

    @Test
    fun `isValid returns false for null email`() {
        assertFalse(EmailValidator.isValid(null), "isValid should return false for null email")
    }

    // Empty email test
    @Test
    fun `validate returns failure with EMAIL_EMPTY for empty string`() {
        val result = EmailValidator.validate("")
        assertFalse(result.isValid, "Empty email should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.EMAIL_EMPTY, "Should contain EMAIL_EMPTY error")
    }

    @Test
    fun `isValid returns false for empty email`() {
        assertFalse(EmailValidator.isValid(""), "isValid should return false for empty email")
    }

    // Invalid email format tests
    @Test
    fun `validate returns failure with EMAIL_INVALID for missing at symbol`() {
        val result = EmailValidator.validate("userexample.com")
        assertFalse(result.isValid, "Email without @ should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure with EMAIL_INVALID for multiple at symbols`() {
        val result = EmailValidator.validate("user@@example.com")
        assertFalse(result.isValid, "Email with multiple @ should fail")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure with EMAIL_INVALID for missing local part`() {
        val result = EmailValidator.validate("@example.com")
        assertFalse(result.isValid, "Email without local part should fail")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure with EMAIL_INVALID for missing domain`() {
        val result = EmailValidator.validate("user@")
        assertFalse(result.isValid, "Email without domain should fail")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure with EMAIL_INVALID for spaces in email`() {
        val result = EmailValidator.validate("user name@example.com")
        assertFalse(result.isValid, "Email with spaces should fail")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure with EMAIL_INVALID for invalid characters in domain`() {
        val result = EmailValidator.validate("user@exam ple.com")
        assertFalse(result.isValid, "Email with spaces in domain should fail")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure with EMAIL_INVALID for domain with invalid special chars`() {
        val result = EmailValidator.validate("user@example$.com")
        assertFalse(result.isValid, "Email with $ in domain should fail")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `isValid returns false for invalid email format`() {
        assertFalse(EmailValidator.isValid("invalid.email"), "isValid should return false for invalid format")
    }
}
