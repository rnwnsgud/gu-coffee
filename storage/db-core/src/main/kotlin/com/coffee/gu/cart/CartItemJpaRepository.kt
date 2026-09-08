package com.coffee.gu.cart

import org.springframework.data.jpa.repository.JpaRepository

interface CartItemJpaRepository : JpaRepository<CartItemEntity, Long>
