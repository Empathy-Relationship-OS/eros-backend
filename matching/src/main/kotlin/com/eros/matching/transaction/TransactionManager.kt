package com.eros.matching.transaction

import com.eros.database.dbQuery
import org.jetbrains.exposed.v1.core.Transaction

/**
 * Abstraction for database transaction management.
 *
 * This interface allows the service layer to be testable without requiring
 * a real database connection. Production code uses DatabaseTransactionManager
 * which delegates to dbQuery, while tests use NoOpTransactionManager (from test sources).
 */
interface TransactionManager {
    /**
     * Executes the given block within a transaction context.
     *
     * @param block The code to execute within the transaction (as a Transaction extension)
     * @return The result of executing the block
     */
    suspend fun <T> execute(block: suspend Transaction.() -> T): T
}

/**
 * Production implementation that delegates to the existing dbQuery function.
 *
 * This ensures all database operations run within proper Exposed transactions
 * with connection pooling and transaction management.
 */
class DatabaseTransactionManager : TransactionManager {
    override suspend fun <T> execute(block: suspend Transaction.() -> T): T = dbQuery(block)
}
