package com.eros.auth.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * OTP Verification table - Phone number verification via one-time passwords
 *
 * This table stores OTP verification attempts for phone number confirmation.
 * OTPs should be hashed before storage (never store plaintext).
 *
 * Typical flow:
 * 1. Generate OTP, hash it, store with expiration (5-10 minutes)
 * 2. User submits OTP, hash and compare
 * 3. Increment attempts on failed verification
 * 4. Mark user as verified on success
 */
@OptIn(ExperimentalUuidApi::class)
object OtpVerification : Table("otp_verification") {

    val id = uuid("id").autoGenerate()
    val phoneNumber = varchar("phone_number", 20)
    val otpHash = varchar("otp_hash", 255)
    val expiresAt = timestamp("expires_at")
    val attempts = integer("attempts").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        // Indexes defined in migration, documented here for reference
        index(customIndexName = "idx_otp_phone_number", columns = arrayOf(phoneNumber))
        index(customIndexName = "idx_otp_expires_at", columns = arrayOf(expiresAt))
    }
}

/**
 * OTP Verification DTO - Immutable data transfer object
 */
@OptIn(ExperimentalUuidApi::class)
data class OtpVerificationRecord(
    val id: Uuid,
    val phoneNumber: String,
    val otpHash: String,
    val expiresAt: Instant,
    val attempts: Int,
    val createdAt: Instant
) {
    /**
     * Check if this OTP has expired
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    /**
     * Check if maximum attempts have been reached
     */
    fun hasExceededAttempts(): Boolean = attempts >= MAX_ATTEMPTS

    companion object {
        const val MAX_ATTEMPTS = 10
        const val DEFAULT_EXPIRY_MINUTES = 10L
    }
}

/**
 * Extension function to map ResultRow to OtpVerificationRecord DTO
 */
@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toOtpVerificationRecord() = OtpVerificationRecord(
    id = this[OtpVerification.id],
    phoneNumber = this[OtpVerification.phoneNumber],
    otpHash = this[OtpVerification.otpHash],
    expiresAt = this[OtpVerification.expiresAt],
    attempts = this[OtpVerification.attempts],
    createdAt = this[OtpVerification.createdAt]
)
