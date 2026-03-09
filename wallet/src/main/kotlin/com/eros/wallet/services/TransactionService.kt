package com.eros.wallet.services

import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.repository.TransactionRepository
import java.time.Clock

class TransactionService(
    private val repository: TransactionRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /*
    suspend fun createPurchaseTransaction(
        userId: String,
        tokenAmount: Double,
        amountPaidGBP: Double,
        stripePaymentIntentId: String,
        status: TransactionStatus){

        // Get users wallet
        Transaction(
            0L,
            userId,
            TransactionType.PURCHASE,
            tokenAmount,
            balanceAfter =
        )

    } //for token purchases
    */


    suspend fun createSpendTransaction(){} //for date commitments


    suspend fun createRefundTransaction(){} //for refunds


}