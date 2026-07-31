package com.coffee.gu.order;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface OrderLineRepository {
    List<OrderLine> saveAll(List<OrderLine> orderLines);
    List<OrderLine> findByOrderKey(Collection<String> orderKeys);
    default List<OrderLine> findByOrderKey(String orderKey) {
        if (orderKey == null) return Collections.emptyList();
        return findByOrderKey(Collections.singletonList(orderKey));
    }

}
