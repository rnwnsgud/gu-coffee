package com.coffee.gu.store;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreService {

    private final StoreReader storeReader;

    public StoreService(StoreReader storeReader) {
        this.storeReader = storeReader;
    }

    public List<Store> getAroundStores(StoreSearch storeSearch) {
        return storeReader.getAroundStores(storeSearch);
    }
}
