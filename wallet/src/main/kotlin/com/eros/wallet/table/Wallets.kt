package com.eros.wallet.table

import com.eros.users.table.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant

object Wallets : Table("wallets") {
    val userId = varchar("user_id", 128).references(Users.userId)
    val tokenBalance = decimal("token_balance", 10, 2).default(BigDecimal.ZERO)
    val lifetimeSpent = decimal("lifetime_spent", 10, 2).default(BigDecimal.ZERO)
    val lifetimePurchased = decimal("lifetime_purchased", 10, 2).default(BigDecimal.ZERO)
    val currency = varchar("currency",3).default("GBP")
    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(userId)

    init {
        // Ensure balance is never negative
        check("balance_non_negative") {
            tokenBalance greaterEq BigDecimal.ZERO
        }
    }
}