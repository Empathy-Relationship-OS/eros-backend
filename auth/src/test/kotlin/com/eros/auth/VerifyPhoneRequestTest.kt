package com.eros.auth

import com.eros.auth.models.VerifyPhoneRequest
import com.eros.auth.validation.Errors
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertContains

class VerifyPhoneRequestTest {

    // Valid verification request tests
    @Test
    fun `validate returns success for valid phone and OTP`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "123456"
        )
        val result = request.validate()
        assertTrue(result.isValid, "Valid phone and OTP should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid verification should have no errors")
    }

    @Test
    fun `isValid returns true for valid verification request`() {
        val request = VerifyPhoneRequest(
            phone = "+447700900123",
            otp = "654321"
        )
        assertTrue(request.isValid(), "isValid should return true for valid verification")
    }

    @Test
    fun `validate returns success for various valid phone formats`() {
        val request = VerifyPhoneRequest(
            phone = "+15551234567",
            otp = "999999"
        )
        val result = request.validate()
        assertTrue(result.isValid, "Valid US phone number should pass validation")
    }

    // Invalid phone tests
    @Test
    fun `validate returns failure for phone without plus sign`() {
        val request = VerifyPhoneRequest(
            phone = "12125551234",
            otp = "123456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone without plus should fail validation")
        assertContains(result.errors, Errors.PHONE_PLUS, "Should contain PHONE_PLUS error")
    }

    @Test
    fun `validate returns failure for phone starting with zero`() {
        val request = VerifyPhoneRequest(
            phone = "+01234567890",
            otp = "123456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone starting with 0 should fail validation")
        assertContains(result.errors, Errors.PHONE_ZERO, "Should contain PHONE_ZERO error")
    }

    @Test
    fun `validate returns failure for phone with non-digits`() {
        val request = VerifyPhoneRequest(
            phone = "+1-212-555-1234",
            otp = "123456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone with non-digits should fail validation")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure for empty phone`() {
        val request = VerifyPhoneRequest(
            phone = "",
            otp = "123456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty phone should fail validation")
        assertContains(result.errors, Errors.PHONE_EMPTY, "Should contain PHONE_EMPTY error")
    }

    @Test
    fun `validate returns failure for phone with spaces`() {
        val request = VerifyPhoneRequest(
            phone = "+1 212 555 1234",
            otp = "123456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Phone with spaces should fail validation")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    // Invalid OTP tests
    @Test
    fun `validate returns failure for blank OTP`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = ""
        )
        val result = request.validate()
        assertFalse(result.isValid, "Blank OTP should fail validation")
        assertContains(result.errors, Errors.OTP_BLANK, "Should contain OTP_BLANK error")
    }

    @Test
    fun `validate returns failure for OTP with non-digits`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "12a456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "OTP with non-digits should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    @Test
    fun `validate returns failure for OTP with wrong length`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "12345"
        )
        val result = request.validate()
        assertFalse(result.isValid, "5-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure for OTP that is too long`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "1234567"
        )
        val result = request.validate()
        assertFalse(result.isValid, "7-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure for OTP with special characters`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "123-456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "OTP with special characters should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    @Test
    fun `validate returns failure for OTP with spaces`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "123 456"
        )
        val result = request.validate()
        assertFalse(result.isValid, "OTP with spaces should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    // Multiple errors tests
    @Test
    fun `validate returns multiple errors for invalid phone and OTP`() {
        val request = VerifyPhoneRequest(
            phone = "1234567890",
            otp = "abc"
        )
        val result = request.validate()
        assertFalse(result.isValid, "Invalid phone and OTP should fail validation")
        assertTrue(result.errors.size >= 2, "Should have multiple errors")
        assertContains(result.errors, Errors.PHONE_PLUS, "Should contain PHONE_PLUS error")
        assertTrue(
            result.errors.any { it in listOf(Errors.OTP_NON_DIGITS, Errors.OTP_SIZE) },
            "Should contain OTP validation errors"
        )
    }

    @Test
    fun `validate returns multiple errors for empty phone and empty OTP`() {
        val request = VerifyPhoneRequest(
            phone = "",
            otp = ""
        )
        val result = request.validate()
        assertFalse(result.isValid, "Empty phone and OTP should fail validation")
        assertTrue(result.errors.size >= 2, "Should have at least two errors")
        assertContains(result.errors, Errors.PHONE_EMPTY, "Should contain PHONE_EMPTY error")
        assertContains(result.errors, Errors.OTP_BLANK, "Should contain OTP_BLANK error")
    }

    @Test
    fun `isValid returns false for invalid verification request`() {
        val request = VerifyPhoneRequest(
            phone = "invalid",
            otp = "bad"
        )
        assertFalse(request.isValid(), "isValid should return false for invalid verification")
    }

    // Edge cases
    @Test
    fun `validate accepts OTP with leading zeros`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "000123"
        )
        val result = request.validate()
        assertTrue(result.isValid, "OTP with leading zeros should pass validation")
    }

    @Test
    fun `validate accepts all zeros OTP`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "000000"
        )
        val result = request.validate()
        assertTrue(result.isValid, "All zeros OTP should pass validation")
    }

    @Test
    fun `validate accepts all nines OTP`() {
        val request = VerifyPhoneRequest(
            phone = "+12125551234",
            otp = "999999"
        )
        val result = request.validate()
        assertTrue(result.isValid, "All nines OTP should pass validation")
    }

    @Test
    fun `validate accepts long international phone number`() {
        val request = VerifyPhoneRequest(
            phone = "+441234567890",
            otp = "123456"
        )
        val result = request.validate()
        assertTrue(result.isValid, "Long international phone should pass validation")
    }
}
