package com.eros.wallet.repository

import com.eros.database.repository.IBaseDAO
import com.eros.wallet.models.Transaction

interface TransactionRepository  : IBaseDAO<Long, Transaction> {

    suspend fun findByIdempotencyKey(idempotencyKey: String) : Transaction?
    suspend fun findByUserId(userId: String) : List<Transaction>

}