package com.coffee.gu.store

import org.springframework.stereotype.Service

@Service
class StoreService(
    private val storeReader: StoreReader,
) {
    fun getAroundStores(storeSearch: StoreSearch): List<Store> {
        return storeReader.getAroundStores(storeSearch)
    }
}
