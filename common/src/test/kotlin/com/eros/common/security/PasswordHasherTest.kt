package com.eros.common.security

import kotlin.system.measureTimeMillis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith


class PasswordHasherTest {

    companion object {
        private val HASH_TIME_LIMIT_MS = System.getenv("HASH_TIME_LIMIT_MS")?.toLongOrNull() ?: 500L
        private val HASH_VERIFY_TIME_LIMIT_MS = System.getenv("HASH_VERIFY_TIME_LIMIT_MS")?.toLongOrNull() ?: 1000L
    }

    @Test
    fun `successful password hash within limit (300ms)`() {
        val time = measureTimeMillis {
            val password = "Absdfijjoi\$4ijji*sfreg'okcdEf\$g123!"
            val hashedPassword = PasswordHasher.hash(password)
        }
        assertTrue(time <= HASH_TIME_LIMIT_MS, "Hashing took longer than ${HASH_TIME_LIMIT_MS}ms: ${time}ms")
    }

    @Test
    fun `successful password hash+verify within limit (600ms)`() {
        var result = false
        val time = measureTimeMillis {
            val password = "Absdfijjoi\$4ijji*sfreg'okcdEf\$g123!"
            val hashedPassword = PasswordHasher.hash(password)
            result = PasswordHasher.verify(password, hashedPassword)
        }
        assertTrue(time <= HASH_VERIFY_TIME_LIMIT_MS, "Hashing and verification took longer than ${HASH_VERIFY_TIME_LIMIT_MS}ms: ${time}ms")
        assertTrue(result, "Password and hash did not match when they should have.")
    }

    @Test
    fun `different passwords fail verification`(){
        val password = "GFgft\$%ef&*sef£sefM,!'\$rDAW\$G53"
        val hashedPassword = PasswordHasher.hash(password)
        val result = PasswordHasher.verify("iNcorRecT_P45Sw0Rd!.$£$", hashedPassword)
        assertFalse(result, "Password and hash incorrectly matched.")
    }

    @Test
    fun `empty password throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PasswordHasher.hash("")
        }
        assertEquals(exception.message?.contains("Password cannot be blank or empty"), true)
    }

    @Test
    fun `blank password throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PasswordHasher.hash("   ")
        }
        assertEquals(exception.message?.contains("Password cannot be blank or empty"), true)
    }

    @Test
    fun `whitespace-only password throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PasswordHasher.hash("\t\n  ")
        }
        assertEquals(exception.message?.contains("Password cannot be blank or empty"), true)
    }
}
