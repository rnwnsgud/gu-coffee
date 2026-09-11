package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.store.StoreSearch

class StoreSearchRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double,
) {
    fun toStoreSearch(): StoreSearch {
        return StoreSearch(latitude, longitude, radiusKm)
    }
}
