package com.coffee.gu.stamp

import org.springframework.data.jpa.repository.JpaRepository

interface StampHistoryJpaRepository : JpaRepository<StampHistoryEntity, Long>
