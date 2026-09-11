package com.coffee.gu.store

interface StoreRepository {
    fun save(store: Store): Store
    fun findById(id: Long): Store
    fun findAllByGeography(latitude: Double, longitude: Double, radiusKm: Double): List<Store>
}
