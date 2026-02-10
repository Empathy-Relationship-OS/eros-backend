package com.eros.auth

import com.eros.auth.models.RegisterRequest
import com.eros.auth.validation.Errors
import kotlinx.datetime.toKotlinLocalDate
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisterRequestTest {

    // Valid registration request tests
    @Test
    fun `validate returns success for valid registration`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertTrue(result.isValid, "Valid registration should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid registration should have no errors")
    }

    @Test
    fun `isValid returns true for valid registration`() {
        val request = RegisterRequest(
            email = "test@domain.com",
            password = "ValidP@ssw0rd",
            phone = "+447700900123",
            name = "Jane Smith",
            birthDate = LocalDate.of(1995, 6, 15).toKotlinLocalDate()
        )
        assertTrue(request.isValid(), "isValid should return true for valid registration")
    }

    // Invalid name tests
    @Test
    fun `validate returns failure for blank name`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Blank name should fail validation")
        assertContains(result.errors, Errors.BLANK_NAME, "Should contain BLANK_NAME error")
    }

    @Test
    fun `validate returns failure for whitespace-only name`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "   ",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Whitespace-only name should fail validation")
        assertContains(result.errors, Errors.BLANK_NAME, "Should contain BLANK_NAME error")
    }

    // Invalid email tests
    @Test
    fun `validate returns failure for invalid email`() {
        val request = RegisterRequest(
            email = "not-an-email",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Invalid email should fail validation")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
    }

    @Test
    fun `validate returns failure for empty email`() {
        val request = RegisterRequest(
            email = "",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty email should fail validation")
        assertContains(result.errors, Errors.EMAIL_EMPTY, "Should contain EMAIL_EMPTY error")
    }

    // Invalid password tests
    @Test
    fun `validate returns failure for weak password`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "weak",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Weak password should fail validation")
        assertTrue(result.errors.any { it in listOf(Errors.LENGTH, Errors.UPPER_MISSING, Errors.DIGIT_MISSING, Errors.SPECIAL_MISSING) })
    }

    @Test
    fun `validate returns failure for password with whitespace`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "Has Space123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Password with whitespace should fail validation")
        assertContains(result.errors, Errors.WHITESPACE, "Should contain WHITESPACE error")
    }

    // Invalid phone tests
    @Test
    fun `validate returns failure for phone without plus sign`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone without plus should fail validation")
        assertContains(result.errors, Errors.PHONE_PLUS, "Should contain PHONE_PLUS error")
    }

    @Test
    fun `validate returns failure for phone starting with zero`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+01234567890",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone starting with 0 should fail validation")
        assertContains(result.errors, Errors.PHONE_ZERO, "Should contain PHONE_ZERO error")
    }

    @Test
    fun `validate returns failure for phone with non-digits`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+1-212-555-1234",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone with non-digits should fail validation")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure for empty phone`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty phone should fail validation")
        assertContains(result.errors, Errors.PHONE_EMPTY, "Should contain PHONE_EMPTY error")
    }

    // Invalid age tests
    @Test
    fun `validate returns failure for underage user`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(2010, 1, 1).toKotlinLocalDate()  // 15 years old
        )
        val result = request.validate()
        assertFalse(result.isValid, "Underage user should fail validation")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `validate returns success for user exactly 18 years old`() {
        val today = LocalDate.now(Clock.systemUTC())
        val eighteenYearsAgo = LocalDate.of(today.year - 18, today.monthValue, today.dayOfMonth).toKotlinLocalDate()

        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = eighteenYearsAgo
        )
        val result = request.validate()
        assertTrue(result.isValid, "User exactly 18 years old should pass validation")
    }

    // Multiple errors tests
    @Test
    fun `validate returns multiple errors for completely invalid registration`() {
        val request = RegisterRequest(
            email = "bad-email",
            password = "weak",
            phone = "1234567890",
            name = "",
            birthDate = LocalDate.of(2015, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertFalse(result.isValid, "Invalid registration should fail validation")
        assertTrue(result.errors.size >= 4, "Should have multiple errors")
        assertContains(result.errors, Errors.BLANK_NAME, "Should contain BLANK_NAME error")
        assertContains(result.errors, Errors.EMAIL_INVALID, "Should contain EMAIL_INVALID error")
        assertContains(result.errors, Errors.PHONE_PLUS, "Should contain PHONE_PLUS error")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `isValid returns false for invalid registration`() {
        val request = RegisterRequest(
            email = "bad-email",
            password = "weak",
            phone = "1234567890",
            name = "",
            birthDate = LocalDate.of(2015, 1, 1).toKotlinLocalDate()
        )
        assertFalse(request.isValid(), "isValid should return false for invalid registration")
    }

    // Edge cases
    @Test
    fun `validate accepts valid international phone numbers`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+447700900123",
            name = "John Doe",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertTrue(result.isValid, "Valid UK phone number should pass validation")
    }

    @Test
    fun `validate accepts name with special characters`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "Jean-Pierre O'Neill",
            birthDate = LocalDate.of(2000, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertTrue(result.isValid, "Name with hyphens and apostrophes should pass validation")
    }

    @Test
    fun `validate accepts user who is 50 years old`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "SecurePass123!",
            phone = "+12125551234",
            name = "John Doe",
            birthDate = LocalDate.of(1975, 1, 1).toKotlinLocalDate()
        )
        val result = request.validate()
        assertTrue(result.isValid, "50 year old user should pass validation")
    }
}
