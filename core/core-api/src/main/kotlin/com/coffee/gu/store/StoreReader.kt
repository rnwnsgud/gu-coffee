package com.coffee.gu.store

import org.springframework.stereotype.Component

@Component
class StoreReader(
    private val storeRepository: StoreRepository,
    private val salesHourRepository: SalesHourRepository,
) {
    fun getAroundStores(storeSearch: StoreSearch): List<Store> {
        val stores = storeRepository.findAllByGeography(
            storeSearch.latitude,
            storeSearch.longitude,
            storeSearch.radiusKm
        )
        val salesHours = salesHourRepository.findAllByStores(stores)
        val hoursMap = salesHours.groupBy { it.storeId }

        for (store in stores) {
            val salesInformation = store.salesInformation
            if (salesInformation != null) {
                val storeHours = hoursMap[store.id] ?: emptyList()
                val updatedInfo = SalesInformation(
                    location = salesInformation.location,
                    hours = storeHours,
                    phoneNumber = salesInformation.phoneNumber
                )
                store.fillSalesInformation(updatedInfo)
            }
        }
        return stores
    }
}
