package com.coffee.gu.store;

import java.util.Collection;
import java.util.List;

public interface SalesHourRepository {
    List<SalesHour> findAllByStores(Collection<Store> stores);
}
