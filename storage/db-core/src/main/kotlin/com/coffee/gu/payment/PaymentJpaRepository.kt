package com.coffee.gu.payment

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :paymentId")
    fun findByIdForUpdate(@Param("paymentId") paymentId: Long): PaymentEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.orderKey = :orderKey")
    fun findByOrderIdForUpdate(@Param("orderKey") orderKey: String): PaymentEntity?
}
