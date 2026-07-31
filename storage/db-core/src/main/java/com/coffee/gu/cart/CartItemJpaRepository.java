package com.coffee.gu.cart;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, Long> {
}
