package com.coffee.gu.store;


import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StoreReader {
    
    private final StoreRepository storeRepository;
    private final SalesHourRepository salesHourRepository;

    public StoreReader(StoreRepository storeRepository, SalesHourRepository salesHourRepository) {
        this.storeRepository = storeRepository;
        this.salesHourRepository = salesHourRepository;
    }

    public List<Store> getAroundStores(StoreSearch storeSearch) {
        List<Store> stores = storeRepository.findAllByGeography(storeSearch.latitude(), storeSearch.longitude(), storeSearch.radiusKm());
        List<SalesHour> salesHours = salesHourRepository.findAllByStores(stores);
        Map<Long, List<SalesHour>> hoursMap = salesHours.stream()
                .collect(Collectors.groupingBy(SalesHour::storeId));
        for (Store store : stores) {
            if (store.getSalesInformation() != null) {
                List<SalesHour> storeHours = hoursMap.getOrDefault(store.getId(), List.of());
                SalesInformation updatedInfo = new SalesInformation(
                        store.getSalesInformation().location(),
                        storeHours,
                        store.getSalesInformation().phoneNumber()
                );
                store.fillSalesInformation(updatedInfo);
            }
        }
        return stores;
    }
}
