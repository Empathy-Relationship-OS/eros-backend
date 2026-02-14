package com.eros.auth.repository

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for AuthRepository using Testcontainers PostgreSQL.
 *
 * Tests Firebase-integrated AuthRepository methods with a real PostgreSQL database
 * to ensure proper Exposed ORM integration and transaction management.
 *
 * Note: This tests the backend repository only. Firebase authentication itself
 * is tested separately or mocked.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        }
    }

    // ========== createOrUpdateUser() tests ==========

    @Test
    fun `createOrUpdateUser creates new user with Firebase UID`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        val email = "test@example.com"
        val phone = "+1234567890"

        // When
        val user = repository.createOrUpdateUser(firebaseUid, email, phone).user

        // Then
        assertEquals(firebaseUid, user.id)
        assertEquals(email, user.email)
        assertEquals(phone, user.phone)
        assertNotNull(user.createdAt)
        assertNotNull(user.updatedAt)
        assertNull(user.lastActiveAt)
    }

    @Test
    fun `createOrUpdateUser allows null phone number`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        val email = "test@example.com"

        // When
        val user = repository.createOrUpdateUser(firebaseUid, email, null).user

        // Then
        assertEquals(firebaseUid, user.id)
        assertEquals(email, user.email)
        assertNull(user.phone)
    }

    @Test
    fun `createOrUpdateUser updates existing user`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        val originalEmail = "original@example.com"
        val updatedEmail = "updated@example.com"
        val updatedPhone = "+9876543210"

        // Create initial user
        val originalUser = repository.createOrUpdateUser(firebaseUid, originalEmail, null).user
        val originalCreatedAt = originalUser.createdAt

        // Advance time
        testClock.advance(Duration.ofHours(24))

        // When - update with new email and phone
        val updatedUser = repository.createOrUpdateUser(firebaseUid, updatedEmail, updatedPhone).user

        // Then
        assertEquals(firebaseUid, updatedUser.id)
        assertEquals(updatedEmail, updatedUser.email)
        assertEquals(updatedPhone, updatedUser.phone)
        assertTrue(updatedUser.createdAt in originalCreatedAt.minusSeconds(10)..originalCreatedAt.plusSeconds(10), "createdAt should not change on update")
        assertTrue(updatedUser.updatedAt.isAfter(originalUser.updatedAt), "updatedAt should be refreshed")
    }

    @Test
    fun `createOrUpdateUser throws exception for duplicate email with different Firebase UID`() = runTest {
        // Given
        val email = "test@example.com"
        repository.createOrUpdateUser("firebase_uid_1", email, null)

        // When & Then
        assertThrows<org.jetbrains.exposed.v1.exceptions.ExposedSQLException> {
            repository.createOrUpdateUser("firebase_uid_2", email, null)
        }
    }

    @Test
    fun `createOrUpdateUser throws exception for duplicate phone with different Firebase UID`() = runTest {
        // Given
        val phone = "+1234567890"
        repository.createOrUpdateUser("firebase_uid_1", "user1@example.com", phone)

        // When & Then
        assertThrows<org.jetbrains.exposed.v1.exceptions.ExposedSQLException> {
            repository.createOrUpdateUser("firebase_uid_2", "user2@example.com", phone)
        }
    }

    // ========== findByFirebaseUid() tests ==========

    @Test
    fun `findByFirebaseUid returns user when exists`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        val email = "test@example.com"
        repository.createOrUpdateUser(firebaseUid, email, "+1234567890")

        // When
        val foundUser = repository.findByFirebaseUid(firebaseUid)

        // Then
        assertNotNull(foundUser)
        assertEquals(firebaseUid, foundUser.id)
        assertEquals(email, foundUser.email)
    }

    @Test
    fun `findByFirebaseUid returns null when user does not exist`() = runTest {
        // When
        val foundUser = repository.findByFirebaseUid("nonexistent_uid")

        // Then
        assertNull(foundUser)
    }

    // ========== findByEmail() tests ==========

    @Test
    fun `findByEmail returns user when exists`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        val email = "test@example.com"
        repository.createOrUpdateUser(firebaseUid, email, "+1234567890")

        // When
        val foundUser = repository.findByEmail(email)

        // Then
        assertNotNull(foundUser)
        assertEquals(firebaseUid, foundUser.id)
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
    fun `findByEmail is not case-sensitive`() = runTest {
        // Given
        repository.createOrUpdateUser("firebase_uid_123", "test@example.com", "+1234567890")

        // When
        val foundUser = repository.findByEmail("TEST@EXAMPLE.COM")

        // Then
        assertNotNull(foundUser, "Email lookup should be case-sensitive")
    }

    // ========== updateLastActiveAt() tests ==========

    @Test
    fun `updateLastActiveAt sets last active timestamp`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        val user = repository.createOrUpdateUser(firebaseUid, "test@example.com", "+1234567890").user
        assertNull(user.lastActiveAt, "Initially lastActiveAt should be null")

        // When
        val before = Instant.now(testClock)
        val rowsUpdated = repository.updateLastActiveAt(firebaseUid)
        val after = Instant.now(testClock)

        // Then
        assertEquals(1, rowsUpdated)

        val updatedUser = repository.findByFirebaseUid(firebaseUid)
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
        val nonExistentUid = "nonexistent_firebase_uid"

        // When
        val rowsUpdated = repository.updateLastActiveAt(nonExistentUid)

        // Then
        assertEquals(0, rowsUpdated)
    }

    // ========== deleteUser() tests ==========

    @Test
    fun `deleteUser removes user from database`() = runTest {
        // Given
        val firebaseUid = "firebase_uid_123"
        repository.createOrUpdateUser(firebaseUid, "test@example.com", "+1234567890")

        // Verify user exists
        assertNotNull(repository.findByFirebaseUid(firebaseUid))

        // When
        val rowsDeleted = repository.deleteUser(firebaseUid)

        // Then
        assertEquals(1, rowsDeleted)
        assertNull(repository.findByFirebaseUid(firebaseUid), "User should be deleted")
    }

    @Test
    fun `deleteUser returns 0 for non-existent user`() = runTest {
        // Given
        val nonExistentUid = "nonexistent_firebase_uid"

        // When
        val rowsDeleted = repository.deleteUser(nonExistentUid)

        // Then
        assertEquals(0, rowsDeleted)
    }

    @Test
    fun `deleteUser allows email and phone to be reused after deletion`() = runTest {
        // Given
        val firebaseUid1 = "firebase_uid_1"
        val firebaseUid2 = "firebase_uid_2"
        val email = "test@example.com"
        val phone = "+1234567890"

        repository.createOrUpdateUser(firebaseUid1, email, phone)

        // When - delete first user
        repository.deleteUser(firebaseUid1)

        // Then - should be able to create new user with same email and phone
        val newUser = repository.createOrUpdateUser(firebaseUid2, email, phone).user
        assertEquals(firebaseUid2, newUser.id)
        assertEquals(email, newUser.email)
        assertEquals(phone, newUser.phone)
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
