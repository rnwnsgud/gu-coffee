package com.coffee.gu.store

@JvmRecord
data class StoreLocation(
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
