package com.coffee.gu.cancel

import org.springframework.data.jpa.repository.JpaRepository

interface CancelJpaRepository : JpaRepository<CancelEntity, Long>
