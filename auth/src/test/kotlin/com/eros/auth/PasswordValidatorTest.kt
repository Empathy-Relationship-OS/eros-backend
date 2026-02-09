package com.eros.auth

import com.eros.auth.validation.PasswordValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PasswordValidatorTest {

    @Test
    fun validIsValidPassword(){
        val validPassword = "Qwerty123£!"
        val res = PasswordValidator.isValid(validPassword)
        assertTrue(res, "Valid Password did not verify.")

    }

    @Test
    fun invalidIsValidPassword(){
        val validPassword = "qwerty123£!"
        val res = PasswordValidator.isValid(validPassword)
        assertFalse(res, "Invalid Password verified when it shouldn't have.")
    }

    @Test
    fun validValidatePassword(){
        val validPassword = "Qwerty123£!"
        val res = PasswordValidator.validate(validPassword)
        assertTrue(res.isValid, "Valid Password did not verify.")
        assertTrue(res.errors.isEmpty(), "Errors returned when expected none.")
    }

    @Test
    fun invalidValidatePassword(){
        val validPassword = "qwerty"
        val res = PasswordValidator.validate(validPassword)
        assertFalse(res.isValid, "Invalid Password verified when it shouldn't have.")
        assertFalse(res.errors.isEmpty(), "No errors returned when expected to.")
    }
}