package com.eros.common.security

import kotlin.system.measureTimeMillis

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse


class PasswordHasherTest {

    @Test
    fun `successful password hash within limit (300ms)`() {
        val time = measureTimeMillis {
            val password = "Absdfijjoi\$4ijji*sfreg'okcdEf\$g123!"
            val hashedPassword = PasswordHasher.hash(password)
        }
        assertTrue(time <= 300, "Hashing took longer than 300ms: ${time}ms")
    }

    @Test
    fun `successful password hash+verify within limit (600ms)`() {
        var result = false
        val time = measureTimeMillis {
            val password = "Absdfijjoi\$4ijji*sfreg'okcdEf\$g123!"
            val hashedPassword = PasswordHasher.hash(password)
            result = PasswordHasher.verify(password, hashedPassword)
        }
        assertTrue(time <= 600, "Hashing and verification took longer than 600ms: ${time}ms")
        assertTrue(result, "Password and hash did not match when they should have.")
    }

    @Test
    fun `different passwords fail verification`(){
        val password = "GFgft\$%ef&*sef£sefM,!'\$rDAW\$G53"
        val hashedPassword = PasswordHasher.hash(password)
        val result = PasswordHasher.verify("iNcorRecT_P45Sw0Rd!.$£$", hashedPassword)
        assertFalse(result, "Password and hash incorrectly matched.")
    }
}
