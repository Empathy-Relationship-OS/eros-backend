package com.eros.matching.tables

import com.eros.users.table.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Exposed table definition for tracking daily batch fetches per user.
 *
 * Tracks how many batches a user has fetched on a given date to enforce
 * the 3-batches-per-day limit. Composite primary key of (user_id, batch_date).
 */
object UserDailyBatches : Table("user_daily_batches") {
    /** User who fetched batches (references users table) */
    val userId = varchar("user_id", 128).references(Users.userId)

    /** Date of batch fetches (UTC) */
    val batchDate = date("batch_date")

    /** Number of batches fetched on this date (0-3) */
    val batchCount = integer("batch_count").default(0)

    /** When the first batch was fetched for this date */
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    /** When the record was last updated */
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(userId, batchDate)

    init {
        // Indexes for query optimization (defined in migration)
        index("idx_user_daily_batches_user_id", false, userId)
        index("idx_user_daily_batches_date", false, batchDate)
    }
}
