package com.coffee.gu.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LimitedCouponJpaRepository : JpaRepository<LimitedCouponEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lc FROM LimitedCouponEntity lc WHERE lc.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): LimitedCouponEntity?
}
