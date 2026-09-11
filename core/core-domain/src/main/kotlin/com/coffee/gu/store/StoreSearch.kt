package com.coffee.gu.store

@JvmRecord
data class StoreSearch(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double,
)
