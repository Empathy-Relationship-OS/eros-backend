package com.eros.auth

import com.eros.auth.validation.PhoneValidator
import com.eros.auth.validation.Errors
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertNull

class PhoneValidatorTest {

    // Valid phone number tests
    @Test
    fun `validate returns success for valid US phone number`() {
        val result = PhoneValidator.validate("+14155552671")
        assertTrue(result.isValid, "Valid US phone number should pass")
        assertTrue(result.errors.isEmpty(), "Valid phone number should have no errors")
    }

    @Test
    fun `validate returns success for valid UK phone number`() {
        val result = PhoneValidator.validate("+442071838750")
        assertTrue(result.isValid, "Valid UK phone number should pass")
    }

    @Test
    fun `validate returns success for valid China phone number`() {
        val result = PhoneValidator.validate("+861012345678")
        assertTrue(result.isValid, "Valid China phone number should pass")
    }

    @Test
    fun `validate returns success for minimum length phone number`() {
        val result = PhoneValidator.validate("+1")
        assertTrue(result.isValid, "Minimum length phone number should pass")
    }

    @Test
    fun `validate returns success for maximum length phone number`() {
        val result = PhoneValidator.validate("+123456789012345")
        assertTrue(result.isValid, "Maximum length (15 digits) phone number should pass")
    }

    @Test
    fun `validate returns success for short country code`() {
        val result = PhoneValidator.validate("+11234567890")
        assertTrue(result.isValid, "Phone number with 1-digit country code should pass")
    }

    @Test
    fun `validate returns success for medium country code`() {
        val result = PhoneValidator.validate("+441234567890")
        assertTrue(result.isValid, "Phone number with 2-digit country code should pass")
    }

    @Test
    fun `validate returns success for long country code`() {
        val result = PhoneValidator.validate("+8611234567890")
        assertTrue(result.isValid, "Phone number with 3-digit country code should pass")
    }

    @Test
    fun `isValid returns true for valid phone number`() {
        assertTrue(PhoneValidator.isValid("+14155552671"), "isValid should return true for valid phone")
    }

    // Null phone number test
    @Test
    fun `validate returns failure with PHONE_NULL for null phone number`() {
        val result = PhoneValidator.validate(null)
        assertFalse(result.isValid, "Null phone number should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_NULL, "Should contain PHONE_NULL error")
    }

    @Test
    fun `isValid returns false for null phone number`() {
        assertFalse(PhoneValidator.isValid(null), "isValid should return false for null phone")
    }

    // Empty phone number test
    @Test
    fun `validate returns failure with PHONE_EMPTY for empty string`() {
        val result = PhoneValidator.validate("")
        assertFalse(result.isValid, "Empty phone number should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_EMPTY, "Should contain PHONE_EMPTY error")
    }

    @Test
    fun `isValid returns false for empty phone number`() {
        assertFalse(PhoneValidator.isValid(""), "isValid should return false for empty phone")
    }

    // Missing plus sign test
    @Test
    fun `validate returns failure with PHONE_PLUS for phone without plus sign`() {
        val result = PhoneValidator.validate("14155552671")
        assertFalse(result.isValid, "Phone number without + should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_PLUS, "Should contain PHONE_PLUS error")
    }

    @Test
    fun `validate returns failure with PHONE_PLUS for phone starting with number`() {
        val result = PhoneValidator.validate("441234567890")
        assertFalse(result.isValid, "Phone number starting with digit should fail")
        assertContains(result.errors, Errors.PHONE_PLUS, "Should contain PHONE_PLUS error")
    }

    // Phone with only plus sign test
    @Test
    fun `validate returns failure with PHONE_EMPTY for phone with only plus sign`() {
        val result = PhoneValidator.validate("+")
        assertFalse(result.isValid, "Phone with only + should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_EMPTY, "Should contain PHONE_EMPTY error")
    }

    // Non-digit characters test
    @Test
    fun `validate returns failure with PHONE_DIGITS for phone with spaces`() {
        val result = PhoneValidator.validate("+1 415 555 2671")
        assertFalse(result.isValid, "Phone with spaces should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure with PHONE_DIGITS for phone with hyphens`() {
        val result = PhoneValidator.validate("+1-415-555-2671")
        assertFalse(result.isValid, "Phone with hyphens should fail")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure with PHONE_DIGITS for phone with parentheses`() {
        val result = PhoneValidator.validate("+1(415)5552671")
        assertFalse(result.isValid, "Phone with parentheses should fail")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure with PHONE_DIGITS for phone with dots`() {
        val result = PhoneValidator.validate("+1.415.555.2671")
        assertFalse(result.isValid, "Phone with dots should fail")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure with PHONE_DIGITS for phone with letters`() {
        val result = PhoneValidator.validate("+1415555ABCD")
        assertFalse(result.isValid, "Phone with letters should fail")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    @Test
    fun `validate returns failure with PHONE_DIGITS for phone with special characters`() {
        val result = PhoneValidator.validate("+1415555@#$%")
        assertFalse(result.isValid, "Phone with special characters should fail")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }

    // Starting with zero test
    @Test
    fun `validate returns failure with PHONE_ZERO for phone starting with zero after plus`() {
        val result = PhoneValidator.validate("+0123456789")
        assertFalse(result.isValid, "Phone starting with +0 should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_ZERO, "Should contain PHONE_ZERO error")
    }

    @Test
    fun `validate returns failure with PHONE_ZERO for phone with leading zero`() {
        val result = PhoneValidator.validate("+01234567890")
        assertFalse(result.isValid, "Phone with +0 should fail")
        assertContains(result.errors, Errors.PHONE_ZERO, "Should contain PHONE_ZERO error")
    }

    // Phone too long test
    @Test
    fun `validate returns failure with PHONE_LONG for phone exceeding max length`() {
        val result = PhoneValidator.validate("+1234567890123456")
        assertFalse(result.isValid, "Phone with 16 digits should fail")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.PHONE_LONG, "Should contain PHONE_LONG error")
    }

    @Test
    fun `validate returns failure with PHONE_LONG for very long phone number`() {
        val result = PhoneValidator.validate("+12345678901234567890")
        assertFalse(result.isValid, "Very long phone number should fail")
        assertContains(result.errors, Errors.PHONE_LONG, "Should contain PHONE_LONG error")
    }

    // Normalize function tests
    @Test
    fun `normalize removes spaces from phone number`() {
        val result = PhoneValidator.normalize("+1 415 555 2671")
        assertEquals("+14155552671", result, "Should remove spaces")
    }

    @Test
    fun `normalize removes hyphens from phone number`() {
        val result = PhoneValidator.normalize("+1-415-555-2671")
        assertEquals("+14155552671", result, "Should remove hyphens")
    }

    @Test
    fun `normalize removes parentheses from phone number`() {
        val result = PhoneValidator.normalize("+1(415)555-2671")
        assertEquals("+14155552671", result, "Should remove parentheses and hyphens")
    }

    @Test
    fun `normalize removes dots from phone number`() {
        val result = PhoneValidator.normalize("+1.415.555.2671")
        assertEquals("+14155552671", result, "Should remove dots")
    }

    @Test
    fun `normalize removes all formatting characters`() {
        val result = PhoneValidator.normalize("+1 (415) 555-2671")
        assertEquals("+14155552671", result, "Should remove all formatting")
    }

    @Test
    fun `normalize returns null for null input`() {
        val result = PhoneValidator.normalize(null)
        assertNull(result, "Should return null for null input")
    }

    @Test
    fun `normalize does not modify already normalized number`() {
        val result = PhoneValidator.normalize("+14155552671")
        assertEquals("+14155552671", result, "Should not modify normalized number")
    }

    // Integration test: normalize then validate
    @Test
    fun `normalize and validate works for formatted phone number`() {
        val formatted = "+1 (415) 555-2671"
        val normalized = PhoneValidator.normalize(formatted)
        val result = PhoneValidator.validate(normalized)
        assertTrue(result.isValid, "Normalized phone should be valid")
    }

    @Test
    fun `normalize and validate works for phone with spaces`() {
        val formatted = "+123 456 789"
        val normalized = PhoneValidator.normalize(formatted)
        assertTrue(PhoneValidator.isValid(normalized), "Normalized phone should be valid")
    }

    // Helper method tests
    @Test
    fun `getMinDigits returns correct minimum`() {
        assertEquals(1, PhoneValidator.getMinDigits(), "Minimum digits should be 1")
    }

    @Test
    fun `getMaxDigits returns correct maximum`() {
        assertEquals(15, PhoneValidator.getMaxDigits(), "Maximum digits should be 15")
    }

    @Test
    fun `getFormatInfo returns non-empty format description`() {
        val info = PhoneValidator.getFormatInfo()
        assertTrue(info.isNotEmpty(), "Format info should not be empty")
        assertTrue(info.contains("E.164"), "Should mention E.164 format")
        assertTrue(info.contains("+"), "Should mention plus sign requirement")
    }

    // Edge cases
    @Test
    fun `validate accepts phone number with digit 9 repeated`() {
        val result = PhoneValidator.validate("+999999999")
        assertTrue(result.isValid, "Phone with repeated 9s should be valid")
    }

    @Test
    fun `validate accepts phone number with varied digits`() {
        val result = PhoneValidator.validate("+12345678901234")
        assertTrue(result.isValid, "Phone with varied digits should be valid")
    }

    @Test
    fun `validate rejects phone with mixed valid and invalid characters`() {
        val result = PhoneValidator.validate("+1234-5678")
        assertFalse(result.isValid, "Phone with hyphen should fail")
        assertContains(result.errors, Errors.PHONE_DIGITS, "Should contain PHONE_DIGITS error")
    }
}
