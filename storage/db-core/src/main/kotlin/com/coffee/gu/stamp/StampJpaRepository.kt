package com.coffee.gu.stamp

import org.springframework.data.jpa.repository.JpaRepository

interface StampJpaRepository : JpaRepository<StampEntity, Long>
