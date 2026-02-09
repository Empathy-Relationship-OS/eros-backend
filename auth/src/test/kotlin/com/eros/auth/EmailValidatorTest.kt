package com.eros.auth

import com.eros.auth.validation.EmailValidator
import com.eros.auth.validation.Errors
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailValidatorTest {

    @Test
    fun `valid email passes`() {
        val result = EmailValidator.validate("user.name+tag@example.com")
        assertTrue(result.isValid , "Valid email failed to validate.")
    }

    @Test
    fun `valid email passes2`() {
        val result = EmailValidator.validate("a@e.uk")
        assertTrue(result.isValid , "Valid email failed to validate.")
    }

    @Test
    fun `valid email passes3`() {
        assertTrue(EmailValidator.isValid("%@esef.org"), "Valid email failed to validate.")
    }


    @Test
    fun `invalid email fails`() {
        val result = EmailValidator.validate("user@@example..com")
        assertFalse(result.isValid , "Invalid email failed validated.")
    }


    @Test
    fun `null email fails`() {
        val result = EmailValidator.validate(null)
        if (!result.errors.isEmpty()) {
            assertFalse(result.errors[0] == Errors.EMAIL_INVALID, "Valid email failed to validate.")
        }else{
            assertFalse(result.isValid , "Null email validated with no errors.")
        }
    }
}
