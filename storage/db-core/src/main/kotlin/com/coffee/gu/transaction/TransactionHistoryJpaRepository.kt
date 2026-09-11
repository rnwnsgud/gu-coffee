package com.coffee.gu.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface TransactionHistoryJpaRepository : JpaRepository<TransactionHistoryEntity, Long>
