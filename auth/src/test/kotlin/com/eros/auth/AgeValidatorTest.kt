package com.eros.auth

import com.eros.auth.validation.AgeValidator
import com.eros.auth.validation.Errors
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertContains


class AgeValidatorTest {

    // Valid age tests (18+)
    @Test
    fun `validate returns success for exactly 18 years old`() {
        val birthDate = LocalDate.now().minusYears(18)
        val result = AgeValidator.validate(birthDate)
        assertTrue(result.isValid, "18 years old should pass validation")
        assertTrue(result.errors.isEmpty(), "Valid age should have no errors")
    }

    @Test
    fun `validate returns success for 25 years old`() {
        val birthDate = LocalDate.now().minusYears(25)
        val result = AgeValidator.validate(birthDate)
        assertTrue(result.isValid, "25 years old should pass validation")
    }

    @Test
    fun `validate returns success for 50 years old`() {
        val birthDate = LocalDate.now().minusYears(50)
        val result = AgeValidator.validate(birthDate)
        assertTrue(result.isValid, "50 years old should pass validation")
    }

    @Test
    fun `validate returns success for 100 years old`() {
        val birthDate = LocalDate.now().minusYears(100)
        val result = AgeValidator.validate(birthDate)
        assertTrue(result.isValid, "100 years old should pass validation")
    }

    @Test
    fun `isValid returns true for valid age`() {
        val birthDate = LocalDate.now().minusYears(21)
        assertTrue(AgeValidator.isValid(birthDate), "isValid should return true for 21 years old")
    }

    // Underage tests (under 18)
    @Test
    fun `validate returns failure with UNDERAGE for 17 years old`() {
        val birthDate = LocalDate.now().minusYears(17)
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "17 years old should fail validation")
        assertEquals(1, result.errors.size, "Should have exactly one error")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `validate returns failure with UNDERAGE for 10 years old`() {
        val birthDate = LocalDate.now().minusYears(10)
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "10 years old should fail validation")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `validate returns failure with UNDERAGE for 1 year old`() {
        val birthDate = LocalDate.now().minusYears(1)
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "1 year old should fail validation")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `validate returns failure with UNDERAGE for newborn`() {
        val birthDate = LocalDate.now()
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "Newborn should fail validation")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `validate returns failure with UNDERAGE for 17 years 11 months`() {
        val birthDate = LocalDate.now().minusYears(17).minusMonths(11)
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "17 years 11 months should fail validation")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }

    @Test
    fun `isValid returns false for underage`() {
        val birthDate = LocalDate.now().minusYears(15)
        assertFalse(AgeValidator.isValid(birthDate), "isValid should return false for 15 years old")
    }

    // Edge case: birthday today
    @Test
    fun `validate returns success for turning 18 today`() {
        val birthDate = LocalDate.now().minusYears(18)
        val result = AgeValidator.validate(birthDate)
        assertTrue(result.isValid, "Turning 18 today should pass validation")
    }

    // Future date tests
    @Test
    fun `validate returns failure for future birth date`() {
        val birthDate = LocalDate.now().plusDays(1)
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "Future birth date should fail validation")
        assertContains(result.errors, Errors.UNDERAGE, "Future date should contain UNDERAGE error")
    }

    @Test
    fun `validate returns failure for birth date 10 years in future`() {
        val birthDate = LocalDate.now().plusYears(10)
        val result = AgeValidator.validate(birthDate)
        assertFalse(result.isValid, "Birth date 10 years in future should fail")
        assertContains(result.errors, Errors.UNDERAGE, "Should contain UNDERAGE error")
    }
}
