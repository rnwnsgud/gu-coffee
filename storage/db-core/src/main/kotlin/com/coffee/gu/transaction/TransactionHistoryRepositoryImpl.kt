package com.coffee.gu.transaction

import org.springframework.stereotype.Repository

@Repository
class TransactionHistoryRepositoryImpl(
    private val transactionHistoryJpaRepository: TransactionHistoryJpaRepository,
) : TransactionHistoryRepository {

    override fun save(transactionHistory: TransactionHistory): TransactionHistory {
        return transactionHistoryJpaRepository.save(TransactionHistoryEntity.from(transactionHistory)).toModel()
    }
}
