package com.eros.database.migration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for V1__auth_tables.sql Flyway migration
 *
 * Verifies that:
 * 1. Migration runs successfully
 * 2. Tables are created with correct schema
 * 3. Indexes are created
 * 4. Constraints are applied
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V1AuthTablesMigrationTest {

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
    }

    private lateinit var connection: Connection

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

        // Create JDBC connection for verification
        connection = DriverManager.getConnection(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password
        )
    }

    @AfterAll
    fun teardown() {
        connection.close()
    }

    @Test
    fun `migration V1 runs successfully`() {
        val flyway = Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .load()

        val info = flyway.info()
        val migrations = info.all()

        // Should have V0 (init) and V1 (auth_tables)
        assertTrue(migrations.size >= 2, "Expected at least 2 migrations (V0, V1)")

        val v1Migration = migrations.find { it.version.version == "1" }
        assertNotNull(v1Migration, "V1 migration should exist")
        assertEquals("auth tables", v1Migration.description)
    }

    @Test
    fun `users table exists with correct schema`() {
        val metadata = connection.metaData

        // Verify table exists
        val tables = metadata.getTables(null, null, "users", arrayOf("TABLE"))
        assertTrue(tables.next(), "users table should exist")

        // Verify columns
        val columns = metadata.getColumns(null, null, "users", null)
        val columnNames = mutableListOf<String>()
        while (columns.next()) {
            columnNames.add(columns.getString("COLUMN_NAME"))
        }

        val expectedColumns = listOf("id", "email", "phone", "password_hash", "verification_status", "created_at")
        assertTrue(columnNames.containsAll(expectedColumns),
            "users table should have all expected columns. Found: $columnNames")
    }

    @Test
    fun `users table has unique constraints on email and phone`() {
        val metadata = connection.metaData

        // Check unique constraints
        val indexes = metadata.getIndexInfo(null, null, "users", true, false)
        val uniqueIndexNames = mutableListOf<String>()

        while (indexes.next()) {
            val indexName = indexes.getString("INDEX_NAME")
            uniqueIndexNames.add(indexName)
        }

        assertTrue(uniqueIndexNames.any { it.contains("email") },
            "Should have unique index on email")
        assertTrue(uniqueIndexNames.any { it.contains("phone") },
            "Should have unique index on phone")
    }

    @Test
    fun `users table has indexes on email, phone, verification_status, and created_at`() {
        val statement = connection.createStatement()
        val result = statement.executeQuery("""
            SELECT indexname
            FROM pg_indexes
            WHERE tablename = 'users'
        """)

        val indexes = mutableListOf<String>()
        while (result.next()) {
            indexes.add(result.getString("indexname"))
        }

        assertTrue(indexes.contains("idx_users_email"),
            "Should have idx_users_email index")
        assertTrue(indexes.contains("idx_users_phone"),
            "Should have idx_users_phone index")
        assertTrue(indexes.contains("idx_users_verification_status"),
            "Should have idx_users_verification_status index")
        assertTrue(indexes.contains("idx_users_created_at"),
            "Should have idx_users_created_at index")
    }

    @Test
    fun `otp_verification table exists with correct schema`() {
        val metadata = connection.metaData

        // Verify table exists
        val tables = metadata.getTables(null, null, "otp_verification", arrayOf("TABLE"))
        assertTrue(tables.next(), "otp_verification table should exist")

        // Verify columns
        val columns = metadata.getColumns(null, null, "otp_verification", null)
        val columnNames = mutableListOf<String>()
        while (columns.next()) {
            columnNames.add(columns.getString("COLUMN_NAME"))
        }

        val expectedColumns = listOf("id", "phone_number", "otp_hash", "expires_at", "attempts", "created_at")
        assertTrue(columnNames.containsAll(expectedColumns),
            "otp_verification table should have all expected columns. Found: $columnNames")
    }

    @Test
    fun `otp_verification table has indexes on phone_number and expires_at`() {
        val statement = connection.createStatement()
        val result = statement.executeQuery("""
            SELECT indexname
            FROM pg_indexes
            WHERE tablename = 'otp_verification'
        """)

        val indexes = mutableListOf<String>()
        while (result.next()) {
            indexes.add(result.getString("indexname"))
        }

        assertTrue(indexes.contains("idx_otp_phone_number"),
            "Should have idx_otp_phone_number index")
        assertTrue(indexes.contains("idx_otp_expires_at"),
            "Should have idx_otp_expires_at index")
    }

    @Test
    fun `users table has check constraint on verification_status`() {
        val statement = connection.createStatement()
        val result = statement.executeQuery("""
            SELECT conname, pg_get_constraintdef(oid) as definition
            FROM pg_constraint
            WHERE conrelid = 'users'::regclass
            AND contype = 'c'
        """)

        val constraints = mutableMapOf<String, String>()
        while (result.next()) {
            constraints[result.getString("conname")] = result.getString("definition")
        }

        assertTrue(constraints.containsKey("users_verification_status_check"),
            "Should have verification_status check constraint")

        val constraintDef = constraints["users_verification_status_check"]
        assertNotNull(constraintDef)
        assertTrue(constraintDef.contains("PENDING") &&
                   constraintDef.contains("VERIFIED") &&
                   constraintDef.contains("SUSPENDED"),
            "Check constraint should allow PENDING, VERIFIED, and SUSPENDED")
    }

    @Test
    fun `otp_verification table has check constraint on attempts`() {
        val statement = connection.createStatement()
        val result = statement.executeQuery("""
            SELECT conname, pg_get_constraintdef(oid) as definition
            FROM pg_constraint
            WHERE conrelid = 'otp_verification'::regclass
            AND contype = 'c'
        """)

        val constraints = mutableMapOf<String, String>()
        while (result.next()) {
            constraints[result.getString("conname")] = result.getString("definition")
        }

        assertTrue(constraints.containsKey("otp_attempts_check"),
            "Should have attempts check constraint")

        val constraintDef = constraints["otp_attempts_check"]
        assertNotNull(constraintDef)
        assertTrue(constraintDef.contains("0") && constraintDef.contains("10"),
            "Check constraint should limit attempts between 0 and 10")
    }

    @Test
    fun `can insert valid data into users table`() {
        val statement = connection.createStatement()
        val insertResult = statement.executeUpdate("""
            INSERT INTO users (id, email, password_hash, verification_status)
            VALUES (gen_random_uuid(), 'test@example.com', 'hashed_password', 'PENDING')
        """)

        assertEquals(1, insertResult, "Should insert 1 row")

        val selectResult = statement.executeQuery("SELECT * FROM users WHERE email = 'test@example.com'")
        assertTrue(selectResult.next(), "Should be able to query inserted user")
        assertEquals("PENDING", selectResult.getString("verification_status"))
    }

    @Test
    fun `can insert valid data into otp_verification table`() {
        val statement = connection.createStatement()
        val insertResult = statement.executeUpdate("""
            INSERT INTO otp_verification (id, phone_number, otp_hash, expires_at)
            VALUES (gen_random_uuid(), '+1234567890', 'hashed_otp', NOW() + INTERVAL '10 minutes')
        """)

        assertEquals(1, insertResult, "Should insert 1 row")

        val selectResult = statement.executeQuery("SELECT * FROM otp_verification WHERE phone_number = '+1234567890'")
        assertTrue(selectResult.next(), "Should be able to query inserted OTP record")
        assertEquals(0, selectResult.getInt("attempts"), "Default attempts should be 0")
    }
}
