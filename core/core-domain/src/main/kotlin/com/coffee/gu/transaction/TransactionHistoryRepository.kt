package com.coffee.gu.transaction

interface TransactionHistoryRepository {
    fun save(transactionHistory: TransactionHistory): TransactionHistory
}
