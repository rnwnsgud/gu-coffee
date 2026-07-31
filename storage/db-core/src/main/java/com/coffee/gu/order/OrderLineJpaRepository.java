package com.coffee.gu.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderLineJpaRepository extends JpaRepository<OrderLineEntity, Long> {
    List<OrderLineEntity> findAllByOrderKeyIn(Collection<String> orderKeys);
    List<OrderLineEntity> findByOrderKey(String orderKey);
}
