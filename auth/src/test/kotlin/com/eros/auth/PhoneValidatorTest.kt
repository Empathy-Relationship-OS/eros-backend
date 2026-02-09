package com.eros.auth

import com.eros.auth.validation.PhoneValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneValidatorTest {

    @Test
    fun completeValidTest(){

        val number = "+123 (456) 789"
        val numberNorm = PhoneValidator.normalize(number)
        println(numberNorm)
        assertTrue(PhoneValidator.isValid(numberNorm), "Number failed to normalise and verify.")

    }

    @Test
    fun invalidStartingChar(){
        val number = "123456789"
        assertFalse(PhoneValidator.isValid(number), "Number validated despite not starting with '+'")
    }

    @Test
    fun invalidStartingNumber(){
        val number = "+0123456789"
        assertFalse(PhoneValidator.isValid(number), "Number validated despite starting with '+0'")
    }
}