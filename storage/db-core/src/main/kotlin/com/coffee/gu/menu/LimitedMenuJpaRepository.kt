package com.coffee.gu.menu

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LimitedMenuJpaRepository : JpaRepository<LimitedMenuEntity, Long> {
    fun findByMenuId(menuId: Long): LimitedMenuEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lm FROM LimitedMenuEntity lm WHERE lm.menuId = :menuId")
    fun findByMenuIdForUpdate(@Param("menuId") menuId: Long): LimitedMenuEntity?
}
