package com.eros.matching.tables

import com.eros.users.table.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Exposed table definition for matches between users.
 *
 * Tracks potential matches between two users with like/pass actions and serving timestamps.
 * The table uses an auto-incrementing primary key (match_id) with a unique constraint
 * on the (user1_id, user2_id) pair to prevent duplicate matches.
 */
object Matches : Table("matches") {
    /** Auto-incrementing primary key */
    val matchId = long("match_id").autoIncrement()

    /** First user in the match pair (references users table) */
    val user1Id = varchar("user1_id", 128).references(Users.userId)

    /** Second user in the match pair (references users table) */
    val user2Id = varchar("user2_id", 128).references(Users.userId)

    /** Whether user1 liked user2 */
    val liked = bool("liked").nullable()

    /** When the match record was created */
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    /** When the match record was last updated */
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    /** When the match was served to the user (nullable) */
    val servedAt = timestamp("served_at").nullable()

    override val primaryKey = PrimaryKey(matchId)

    init {
        check("matches_different_users") { user1Id neq user2Id }
        // Unique constraint on (user1_id, user2_id) pair
        uniqueIndex("matches_user_pair_unique", user1Id, user2Id)

        // Additional indexes for query optimization (defined in migration)
        index("idx_matches_user1_id", false, user1Id)
        index("idx_matches_user2_id", false, user2Id)
    }
}
