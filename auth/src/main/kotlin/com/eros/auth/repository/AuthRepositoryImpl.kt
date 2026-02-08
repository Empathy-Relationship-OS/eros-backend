package com.eros.auth.repository

import com.eros.auth.tables.*
import com.eros.common.security.PasswordHasher
import com.eros.database.dbQuery
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Clock
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of AuthRepository using Exposed ORM.
 *
 * All database operations are wrapped in dbQuery for proper transaction management
 * and IO dispatcher execution.
 *
 * @param clock Clock instance for time-based operations (defaults to system UTC)
 */
@OptIn(ExperimentalUuidApi::class)
class AuthRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : AuthRepository {

    override suspend fun createUser(email: String, phone: String?, plainPassword: String): User = dbQuery {
        val passwordHash = PasswordHasher.hash(plainPassword)
        val now = Instant.now(clock)

        val insertStatement = Users.insert { row ->
            row[Users.email] = email
            row[Users.phone] = phone
            row[Users.passwordHash] = passwordHash
            row[Users.verificationStatus] = VerificationStatus.PENDING.name
            row[Users.createdAt] = now
            row[Users.updatedAt] = now
        }

        val insertedId = insertStatement[Users.id]

        Users.selectAll()
            .where { Users.id eq insertedId }
            .single()
            .toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        Users.select(Users.id)
            .where { Users.email eq email }
            .count() > 0
    }

    override suspend fun existsByPhone(phone: String): Boolean = dbQuery {
        Users.select(Users.id)
            .where { Users.phone eq phone }
            .count() > 0
    }

    override suspend fun updateVerificationStatus(userId: Uuid): Int = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[verificationStatus] = VerificationStatus.VERIFIED.name
            it[updatedAt] = Instant.now(clock)
        }
    }

    override suspend fun updateLastActiveAt(userId: Uuid): Int = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[lastActiveAt] = Instant.now(clock)
            it[updatedAt] = Instant.now(clock)
        }
    }

    override suspend fun storeOtp(
        phoneNumber: String,
        plainOtp: String,
        expiryMinutes: Long
    ): Uuid = dbQuery {
        // Hash the OTP before storage
        val otpHash = PasswordHasher.hash(plainOtp)
        val expiresAt = Instant.now(clock).plusSeconds(expiryMinutes * 60)
        val now = Instant.now(clock)

        // Use upsert to guarantee single row per phone number
        val upsertStatement = OtpVerification.upsert(OtpVerification.phoneNumber) {
            it[OtpVerification.phoneNumber] = phoneNumber
            it[OtpVerification.otpHash] = otpHash
            it[OtpVerification.expiresAt] = expiresAt
            it[OtpVerification.attempts] = 0
            it[OtpVerification.createdAt] = now
        }

        upsertStatement[OtpVerification.id]
    }

    override suspend fun verifyOtp(phoneNumber: String, plainOtp: String): OtpVerificationResult = dbQuery {
        // Find the OTP record
        val otpRecord = OtpVerification.selectAll()
            .where { OtpVerification.phoneNumber eq phoneNumber }
            .singleOrNull()
            ?.toOtpVerificationRecord()
            ?: return@dbQuery OtpVerificationResult.NOT_FOUND

        // Check if expired using the injected clock
        val now = Instant.now(clock)
        if (now.isAfter(otpRecord.expiresAt)) {
            // Clean up expired OTP
            OtpVerification.deleteWhere { OtpVerification.id eq otpRecord.id }
            return@dbQuery OtpVerificationResult.EXPIRED
        }

        // Check if max attempts exceeded
        if (otpRecord.hasExceededAttempts()) {
            return@dbQuery OtpVerificationResult.MAX_ATTEMPTS_EXCEEDED
        }

        // Verify the OTP
        val isValid = PasswordHasher.verify(plainOtp, otpRecord.otpHash)

        if (isValid) {
            // Delete OTP on successful verification
            OtpVerification.deleteWhere { OtpVerification.id eq otpRecord.id }
            OtpVerificationResult.SUCCESS
        } else {
            // Atomically increment attempts with guard against exceeding max
            val rowsUpdated = OtpVerification.update({
                (OtpVerification.id eq otpRecord.id) and
                (OtpVerification.attempts less intLiteral(OtpVerificationRecord.MAX_ATTEMPTS))
            }) {
                it[attempts] = OtpVerification.attempts + 1
            }

            // If update affected 0 rows, max attempts was reached
            if (rowsUpdated == 0) {
                OtpVerificationResult.MAX_ATTEMPTS_EXCEEDED
            } else {
                OtpVerificationResult.INVALID
            }
        }
    }
}
