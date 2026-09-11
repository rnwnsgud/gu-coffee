package com.coffee.gu.store

import org.springframework.data.jpa.repository.JpaRepository

interface StoreJpaRepository : JpaRepository<StoreEntity, Long>
