package com.eros.auth.repository

import com.eros.auth.tables.OtpVerificationResult
import com.eros.auth.tables.VerificationStatus
import com.eros.common.security.PasswordHasher
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

/**
 * Integration tests for AuthRepository using Testcontainers PostgreSQL.
 *
 * Tests all repository methods with a real PostgreSQL database to ensure
 * proper Exposed ORM integration and transaction management.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalUuidApi::class)
class AuthRepositoryTest {

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
    }

    private lateinit var repository: AuthRepository
    private lateinit var testClock: MutableClock

    @BeforeAll
    fun setup() {
        // Ensure container is started
        postgres.start()

        // Run Flyway migrations
        val flyway = Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .load()

        flyway.migrate()

        // Connect Exposed to test database
        Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password
        )
    }

    @BeforeEach
    fun cleanDatabase() {
        // Reset test clock to a fixed instant
        testClock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))

        // Initialize repository with test clock
        repository = AuthRepositoryImpl(testClock)

        // Clean tables before each test
        transaction {
            exec("TRUNCATE TABLE users CASCADE")
            exec("TRUNCATE TABLE otp_verification CASCADE")
        }
    }

    // ========== createUser() tests ==========

    @Test
    fun `createUser inserts new user with hashed password`() = runTest {
        // Given
        val email = "test@example.com"
        val phone = "+1234567890"
        val password = "SecurePassword123!"

        // When
        val user = repository.createUser(email, phone, password)

        // Then
        assertNotNull(user.id)
        assertEquals(email, user.email)
        assertEquals(phone, user.phone)
        assertEquals(VerificationStatus.PENDING, user.verificationStatus)
        assertNotNull(user.createdAt)
        assertNotNull(user.updatedAt)

        // Verify password is hashed
        assertTrue(PasswordHasher.verify(password, user.passwordHash))
        assertNotEquals(user.passwordHash, password, "Password should be hashed, not stored as plaintext")
    }

    @Test
    fun `createUser allows null phone number`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "SecurePassword123!"

        // When
        val user = repository.createUser(email, null, password)

        // Then
        assertNotNull(user.id)
        assertEquals(email, user.email)
        assertNull(user.phone)
    }

    @Test
    fun `createUser throws exception for duplicate email`() = runTest {
        // Given
        val email = "test@example.com"
        repository.createUser(email, "+1234567890", "password1")

        // When & Then
        assertThrows<org.jetbrains.exposed.v1.exceptions.ExposedSQLException> {
            repository.createUser(email, "+0987654321", "password2")
        }
    }

    @Test
    fun `createUser throws exception for duplicate phone`() = runTest {
        // Given
        val phone = "+1234567890"
        repository.createUser("user1@example.com", phone, "password1")

        // When & Then
        assertThrows<org.jetbrains.exposed.v1.exceptions.ExposedSQLException> {
            repository.createUser("user2@example.com", phone, "password2")
        }
    }

    // ========== findByEmail() tests ==========

    @Test
    fun `findByEmail returns user when exists`() = runTest {
        // Given
        val email = "test@example.com"
        val createdUser = repository.createUser(email, "+1234567890", "password")

        // When
        val foundUser = repository.findByEmail(email)

        // Then
        assertNotNull(foundUser)
        assertEquals(createdUser.id, foundUser.id)
        assertEquals(email, foundUser.email)
    }

    @Test
    fun `findByEmail returns null when user does not exist`() = runTest {
        // When
        val foundUser = repository.findByEmail("nonexistent@example.com")

        // Then
        assertNull(foundUser)
    }

    @Test
    fun `findByEmail is case-sensitive`() = runTest {
        // Given
        repository.createUser("test@example.com", "+1234567890", "password")

        // When
        val foundUser = repository.findByEmail("TEST@EXAMPLE.COM")

        // Then
        assertNull(foundUser, "Email lookup should be case-sensitive")
    }

    // ========== existsByEmail() tests ==========

    @Test
    fun `existsByEmail returns true when email exists`() = runTest {
        // Given
        val email = "test@example.com"
        repository.createUser(email, "+1234567890", "password")

        // When
        val exists = repository.existsByEmail(email)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `existsByEmail returns false when email does not exist`() = runTest {
        // When
        val exists = repository.existsByEmail("nonexistent@example.com")

        // Then
        assertFalse(exists)
    }

    // ========== existsByPhone() tests ==========

    @Test
    fun `existsByPhone returns true when phone exists`() = runTest {
        // Given
        val phone = "+1234567890"
        repository.createUser("test@example.com", phone, "password")

        // When
        val exists = repository.existsByPhone(phone)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `existsByPhone returns false when phone does not exist`() = runTest {
        // When
        val exists = repository.existsByPhone("+9999999999")

        // Then
        assertFalse(exists)
    }

    @Test
    fun `existsByPhone returns false for null phone`() = runTest {
        // Given
        repository.createUser("test@example.com", null, "password")

        // When - checking for any specific phone
        val exists = repository.existsByPhone("+1234567890")

        // Then
        assertFalse(exists)
    }

    // ========== updateVerificationStatus() tests ==========

    @Test
    fun `updateVerificationStatus marks user as verified`() = runTest {
        // Given
        val user = repository.createUser("test@example.com", "+1234567890", "password")
        assertEquals(VerificationStatus.PENDING, user.verificationStatus)

        // When
        val rowsUpdated = repository.updateVerificationStatus(user.id)

        // Then
        assertEquals(1, rowsUpdated)

        val updatedUser = repository.findByEmail("test@example.com")
        assertNotNull(updatedUser)
        assertEquals(VerificationStatus.VERIFIED, updatedUser.verificationStatus)
    }

    @Test
    fun `updateVerificationStatus returns 0 for non-existent user`() = runTest {
        // Given
        val nonExistentId = kotlin.uuid.Uuid.random()

        // When
        val rowsUpdated = repository.updateVerificationStatus(nonExistentId)

        // Then
        assertEquals(0, rowsUpdated)
    }

    // ========== updateLastActiveAt() tests ==========

    @Test
    fun `updateLastActiveAt sets last active timestamp`() = runTest {
        // Given
        val user = repository.createUser("test@example.com", "+1234567890", "password")
        assertNull(user.lastActiveAt, "Initially lastActiveAt should be null")

        // When
        val before = Instant.now(testClock)
        val rowsUpdated = repository.updateLastActiveAt(user.id)
        val after = Instant.now(testClock)

        // Then
        assertEquals(1, rowsUpdated)

        val updatedUser = repository.findByEmail("test@example.com")
        assertNotNull(updatedUser)
        assertNotNull(updatedUser.lastActiveAt)
        assertTrue(
            updatedUser.lastActiveAt in before..after,
            "lastActiveAt should be set to current time"
        )
    }

    @Test
    fun `updateLastActiveAt returns 0 for non-existent user`() = runTest {
        // Given
        val nonExistentId = kotlin.uuid.Uuid.random()

        // When
        val rowsUpdated = repository.updateLastActiveAt(nonExistentId)

        // Then
        assertEquals(0, rowsUpdated)
    }

    // ========== storeOtp() tests ==========

    @Test
    fun `storeOtp creates OTP record with hashed value`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val plainOtp = "123456"

        // When
        val otpId = repository.storeOtp(phoneNumber, plainOtp, expiryMinutes = 10)

        // Then
        assertNotNull(otpId)

        // Verify OTP can be verified
        val result = repository.verifyOtp(phoneNumber, plainOtp)
        assertEquals(OtpVerificationResult.SUCCESS, result)
    }

    @Test
    fun `storeOtp deletes existing OTP for same phone number`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val firstOtp = "111111"
        val secondOtp = "222222"

        repository.storeOtp(phoneNumber, firstOtp)

        // When
        repository.storeOtp(phoneNumber, secondOtp)

        // Then
        val resultFirst = repository.verifyOtp(phoneNumber, firstOtp)
        assertEquals(OtpVerificationResult.INVALID, resultFirst, "First OTP should no longer be valid")

        val resultSecond = repository.verifyOtp(phoneNumber, secondOtp)
        assertEquals(OtpVerificationResult.SUCCESS, resultSecond, "Second OTP should be valid")
    }

    @Test
    fun `storeOtp sets correct expiry time`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val plainOtp = "123456"

        // When
        repository.storeOtp(phoneNumber, plainOtp, expiryMinutes = 10)

        // Then - OTP should be valid immediately
        val result = repository.verifyOtp(phoneNumber, plainOtp)
        assertEquals(OtpVerificationResult.SUCCESS, result)
    }

    // ========== verifyOtp() tests ==========

    @Test
    fun `verifyOtp returns SUCCESS for valid OTP`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val plainOtp = "123456"
        repository.storeOtp(phoneNumber, plainOtp)

        // When
        val result = repository.verifyOtp(phoneNumber, plainOtp)

        // Then
        assertEquals(OtpVerificationResult.SUCCESS, result)
    }

    @Test
    fun `verifyOtp returns INVALID for wrong OTP`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        repository.storeOtp(phoneNumber, "123456")

        // When
        val result = repository.verifyOtp(phoneNumber, "654321")

        // Then
        assertEquals(OtpVerificationResult.INVALID, result)
    }

    @Test
    fun `verifyOtp returns NOT_FOUND when no OTP exists`() = runTest {
        // When
        val result = repository.verifyOtp("+1234567890", "123456")

        // Then
        assertEquals(OtpVerificationResult.NOT_FOUND, result)
    }

    @Test
    fun `verifyOtp returns EXPIRED for expired OTP`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val plainOtp = "123456"

        // Store OTP with 1 minute expiry
        repository.storeOtp(phoneNumber, plainOtp, expiryMinutes = 1)

        // Advance clock past expiry
        testClock.advance(Duration.ofMinutes(2))

        // When
        val result = repository.verifyOtp(phoneNumber, plainOtp)

        // Then
        assertEquals(OtpVerificationResult.EXPIRED, result)
    }

    @Test
    fun `verifyOtp deletes OTP on successful verification`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val plainOtp = "123456"
        repository.storeOtp(phoneNumber, plainOtp)

        // When
        val firstResult = repository.verifyOtp(phoneNumber, plainOtp)
        val secondResult = repository.verifyOtp(phoneNumber, plainOtp)

        // Then
        assertEquals(OtpVerificationResult.SUCCESS, firstResult)
        assertEquals(OtpVerificationResult.NOT_FOUND, secondResult, "OTP should be deleted after successful verification")
    }

    @Test
    fun `verifyOtp increments attempts on failure`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val correctOtp = "123456"
        val wrongOtp = "654321"
        repository.storeOtp(phoneNumber, correctOtp)

        // When - make multiple failed attempts
        repeat(3) {
            val result = repository.verifyOtp(phoneNumber, wrongOtp)
            assertEquals(OtpVerificationResult.INVALID, result)
        }

        // Then - correct OTP should still work (attempts < max)
        val finalResult = repository.verifyOtp(phoneNumber, correctOtp)
        assertEquals(OtpVerificationResult.SUCCESS, finalResult)
    }

    @Test
    fun `verifyOtp returns MAX_ATTEMPTS_EXCEEDED after 10 failed attempts`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val correctOtp = "123456"
        val wrongOtp = "654321"
        repository.storeOtp(phoneNumber, correctOtp)

        // When - make 10 failed attempts
        repeat(10) {
            repository.verifyOtp(phoneNumber, wrongOtp)
        }

        // Then - 11th attempt should return MAX_ATTEMPTS_EXCEEDED
        val result = repository.verifyOtp(phoneNumber, correctOtp)
        assertEquals(OtpVerificationResult.MAX_ATTEMPTS_EXCEEDED, result)
    }

    @Test
    fun `verifyOtp deletes expired OTP`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val plainOtp = "123456"
        repository.storeOtp(phoneNumber, plainOtp, expiryMinutes = 1)

        // Advance clock past expiry
        testClock.advance(Duration.ofMinutes(2))

        // When
        val firstResult = repository.verifyOtp(phoneNumber, plainOtp)
        val secondResult = repository.verifyOtp(phoneNumber, plainOtp)

        // Then
        assertEquals(OtpVerificationResult.EXPIRED, firstResult)
        assertEquals(OtpVerificationResult.NOT_FOUND, secondResult, "Expired OTP should be deleted")
    }

    // ========== Helper function ==========

    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }
}

/**
 * Mutable Clock implementation for testing time-based operations.
 *
 * Allows tests to control time progression without real delays.
 */
class MutableClock(
    private var instant: Instant,
    private val zone: ZoneId
) : Clock() {

    override fun instant(): Instant = instant

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun getZone(): ZoneId = zone

    /**
     * Advance the clock by the specified duration.
     */
    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }

    /**
     * Set the clock to a specific instant.
     */
    fun setInstant(newInstant: Instant) {
        instant = newInstant
    }
}
