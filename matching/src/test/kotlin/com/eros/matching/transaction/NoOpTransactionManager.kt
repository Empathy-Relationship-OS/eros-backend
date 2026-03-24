package com.eros.matching.transaction

import io.mockk.mockk
import org.jetbrains.exposed.v1.core.Transaction

/**
 * Test implementation of TransactionManager that executes blocks without transaction management.
 *
 * Use this in unit tests where repositories are mocked and no database connection is required.
 *
 * This class lives in test sources so it can use MockK to create a relaxed Transaction mock.
 * Since repositories are mocked in unit tests, the Transaction object is never actually accessed.
 */
class NoOpTransactionManager : TransactionManager {
    private val mockTransaction: Transaction = mockk(relaxed = true)

    override suspend fun <T> execute(block: suspend Transaction.() -> T): T {
        return block(mockTransaction)
    }
}
