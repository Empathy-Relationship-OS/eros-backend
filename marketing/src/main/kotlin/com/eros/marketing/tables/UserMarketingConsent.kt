package com.eros.marketing.tables

import com.eros.users.table.Users
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Exposed table definition for user marketing consent preferences.
 *
 * Tracks whether users have opted in to receive marketing communications.
 * Uses user_id as the primary key since each user has only one marketing consent record.
 */
object UserMarketingConsent : Table("user_marketing_consent") {
    /** User identifier - primary key and foreign key to users table */
    val userId = varchar("user_id", 128).references(Users.userId, onDelete = ReferenceOption.CASCADE)

    /** Whether user has consented to receive marketing communications */
    val marketingConsent = bool("marketing_consent").default(false)

    /** When the consent record was created */
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    /** When the consent record was last updated */
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(userId)
}
