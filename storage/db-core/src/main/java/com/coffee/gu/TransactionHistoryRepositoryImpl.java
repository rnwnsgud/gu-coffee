package com.coffee.gu;

import org.springframework.stereotype.Repository;

@Repository
public class TransactionHistoryRepositoryImpl implements TransactionHistoryRepository{

    private final TransactionHistoryJpaRepository transactionHistoryJpaRepository;

    public TransactionHistoryRepositoryImpl(TransactionHistoryJpaRepository transactionHistoryJpaRepository) {
        this.transactionHistoryJpaRepository = transactionHistoryJpaRepository;
    }

    @Override
    public TransactionHistory save(TransactionHistory transactionHistory) {
        return transactionHistoryJpaRepository.save(TransactionHistoryEntity.from(transactionHistory)).toModel();
    }
}
