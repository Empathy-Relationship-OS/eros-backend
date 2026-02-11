package com.eros.auth

import com.eros.auth.validation.OTPValidator
import com.eros.auth.validation.Errors
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertContains

class OTPValidatorTest {

    // Valid OTP tests (6 digits)
    @Test
    fun `validate returns success for valid 6-digit OTP`() {
        val result = OTPValidator.validate("123456")
        assertTrue(result.isValid, "6-digit OTP should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid OTP should have no errors")
    }

    @Test
    fun `validate returns success for 6-digit OTP with all zeros`() {
        val result = OTPValidator.validate("000000")
        assertTrue(result.isValid, "6-digit OTP with zeros should be valid")
    }

    @Test
    fun `validate returns success for 6-digit OTP with all nines`() {
        val result = OTPValidator.validate("999999")
        assertTrue(result.isValid, "6-digit OTP with nines should be valid")
    }

    @Test
    fun `isValid returns true for valid OTP`() {
        assertTrue(OTPValidator.isValid("654321"), "isValid should return true for valid 6-digit OTP")
    }

    // Blank OTP tests
    @Test
    fun `validate returns failure with OTP_BLANK for empty string`() {
        val result = OTPValidator.validate("")
        assertFalse(result.isValid, "Empty OTP should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.OTP_BLANK, "Should contain OTP_BLANK error")
    }

    @Test
    fun `validate returns failure with OTP_BLANK for whitespace only`() {
        val result = OTPValidator.validate("   ")
        assertFalse(result.isValid, "Whitespace OTP should fail validation")
        assertContains(result.errors, Errors.OTP_BLANK, "Should contain OTP_BLANK error")
    }

    @Test
    fun `validate returns failure with OTP_BLANK for tab characters`() {
        val result = OTPValidator.validate("\t\t")
        assertFalse(result.isValid, "Tab characters should fail validation")
        assertContains(result.errors, Errors.OTP_BLANK, "Should contain OTP_BLANK error")
    }

    @Test
    fun `isValid returns false for blank OTP`() {
        assertFalse(OTPValidator.isValid(""), "isValid should return false for empty OTP")
    }

    // Non-digit tests
    @Test
    fun `validate returns failure with OTP_NON_DIGITS for letters`() {
        val result = OTPValidator.validate("abc123")
        assertFalse(result.isValid, "OTP with letters should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    @Test
    fun `validate returns failure with OTP_NON_DIGITS for special characters`() {
        val result = OTPValidator.validate("123-456")
        assertFalse(result.isValid, "OTP with dash should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    @Test
    fun `validate returns failure with OTP_NON_DIGITS for spaces`() {
        val result = OTPValidator.validate("123 456")
        assertFalse(result.isValid, "OTP with spaces should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    @Test
    fun `validate returns failure with OTP_NON_DIGITS for mixed alphanumeric`() {
        val result = OTPValidator.validate("12a456")
        assertFalse(result.isValid, "OTP with mixed alphanumeric should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    @Test
    fun `validate returns failure with OTP_NON_DIGITS for all letters`() {
        val result = OTPValidator.validate("abcdef")
        assertFalse(result.isValid, "OTP with all letters should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }

    // Length tests (not 6 digits)
    @Test
    fun `validate returns failure with OTP_SIZE for 5 digits`() {
        val result = OTPValidator.validate("12345")
        assertFalse(result.isValid, "5-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure with OTP_SIZE for 7 digits`() {
        val result = OTPValidator.validate("1234567")
        assertFalse(result.isValid, "7-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure with OTP_SIZE for 4 digits`() {
        val result = OTPValidator.validate("1234")
        assertFalse(result.isValid, "4-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure with OTP_SIZE for 3 digits`() {
        val result = OTPValidator.validate("123")
        assertFalse(result.isValid, "3-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure with OTP_SIZE for 1 digit`() {
        val result = OTPValidator.validate("1")
        assertFalse(result.isValid, "1-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `validate returns failure with OTP_SIZE for 10 digits`() {
        val result = OTPValidator.validate("1234567890")
        assertFalse(result.isValid, "10-digit OTP should fail validation")
        assertContains(result.errors, Errors.OTP_SIZE, "Should contain OTP_SIZE error")
    }

    @Test
    fun `isValid returns false for wrong length OTP`() {
        assertFalse(OTPValidator.isValid("12345"), "isValid should return false for 5-digit OTP")
    }

    // Edge cases
    @Test
    fun `validate returns failure for OTP with leading zeros counted correctly`() {
        val result = OTPValidator.validate("000123")
        assertTrue(result.isValid, "OTP with leading zeros should be valid if 6 digits")
    }

    @Test
    fun `validate handles unicode digits correctly`() {
        val result = OTPValidator.validate("①②③④⑤⑥")
        assertFalse(result.isValid, "Unicode digits should fail validation")
        assertContains(result.errors, Errors.OTP_NON_DIGITS, "Should contain OTP_NON_DIGITS error")
    }
}
