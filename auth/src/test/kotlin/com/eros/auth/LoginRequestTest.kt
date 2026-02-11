package com.eros.auth

import com.eros.auth.models.LoginRequest
import com.eros.auth.validation.Errors
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertContains

class LoginRequestTest {

    // Valid login request tests
    @Test
    fun `validate returns success for valid email and password`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "SecurePass123!"
        )
        val result = request.validate()
        assertTrue(result.isValid, "Valid login request should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid login should have no errors")
    }

    @Test
    fun `isValid returns true for valid login request`() {
        val request = LoginRequest(
            email = "test@domain.com",
            password = "ValidP@ssw0rd"
        )
        assertTrue(request.isValid(), "isValid should return true for valid login")
    }

    // Invalid email tests
    @Test
    fun `validate returns failure for invalid email format`() {
        val request = LoginRequest(
            email = "invalid-email",
            password = "ValidPass123!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Invalid email should fail validation")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure for empty email`() {
        val request = LoginRequest(
            email = "",
            password = "ValidPass123!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty email should fail validation")
        assertContains(result.errors, Errors.EMAIL_EMPTY, "Should contain EMAIL_EMPTY error")
    }

    @Test
    fun `validate returns failure for email without domain`() {
        val request = LoginRequest(
            email = "user@",
            password = "ValidPass123!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Email without domain should fail validation")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    // Invalid password tests
    @Test
    fun `validate returns failure for empty password`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = ""
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty password should fail validation")
        assertContains(result.errors, Errors.EMPTY, "Should contain EMPTY error")
    }

    @Test
    fun `validate returns failure for password too short`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "Short1!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Short password should fail validation")
        assertContains(result.errors, Errors.LENGTH, "Should contain LENGTH error")
    }

    @Test
    fun `validate returns failure for password without uppercase`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "lowercase123!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Password without uppercase should fail validation")
        assertContains(result.errors, Errors.UPPER_MISSING, "Should contain UPPER_MISSING error")
    }

    @Test
    fun `validate returns failure for password without lowercase`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "UPPERCASE123!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Password without lowercase should fail validation")
        assertContains(result.errors, Errors.LOWER_MISSING, "Should contain LOWER_MISSING error")
    }

    @Test
    fun `validate returns failure for password without digit`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "NoDigitsHere!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Password without digit should fail validation")
        assertContains(result.errors, Errors.DIGIT_MISSING, "Should contain DIGIT_MISSING error")
    }

    @Test
    fun `validate returns failure for password without special character`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "NoSpecialChar123"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Password without special character should fail validation")
        assertContains(result.errors, Errors.SPECIAL_MISSING, "Should contain SPECIAL_MISSING error")
    }

    @Test
    fun `validate returns failure for password with whitespace`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "Has Space123!"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Password with whitespace should fail validation")
        assertContains(result.errors, Errors.WHITESPACE, "Should contain WHITESPACE error")
    }

    // Multiple errors tests
    @Test
    fun `validate returns multiple errors for both invalid email and password`() {
        val request = LoginRequest(
            email = "not-an-email",
            password = "weak"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Invalid email and password should fail validation")
        assertTrue(result.errors.size > 1, "Should have multiple errors")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
        assertTrue(
            result.errors.any { it in listOf(Errors.LENGTH, Errors.UPPER_MISSING, Errors.DIGIT_MISSING, Errors.SPECIAL_MISSING) },
            "Should contain password validation errors"
        )
    }

    @Test
    fun `validate returns multiple errors for empty email and empty password`() {
        val request = LoginRequest(
            email = "",
            password = ""
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty email and password should fail validation")
        assertEquals(2, result.errors.size, "Should have exactly two errors")
        assertContains(result.errors, Errors.EMAIL_EMPTY, "Should contain EMAIL_EMPTY error")
        assertContains(result.errors, Errors.EMPTY, "Should contain password EMPTY error")
    }

    @Test
    fun `isValid returns false for invalid login request`() {
        val request = LoginRequest(
            email = "bad-email",
            password = "weak"
        )
        assertFalse(request.isValid(), "isValid should return false for invalid login")
    }

    // Edge cases
    @Test
    fun `validate accepts email with special valid characters`() {
        val request = LoginRequest(
            email = "user+test@example.co.uk",
            password = "ValidPass123!"
        )
        val result = request.validate()
        assertTrue(result.isValid, "Valid email with plus sign should pass validation")
    }

    @Test
    fun `validate accepts strong password with various special characters`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "C0mpl3x!P@ssw0rd#2024"
        )
        val result = request.validate()
        assertTrue(result.isValid, "Strong password should pass validation")
    }
}
