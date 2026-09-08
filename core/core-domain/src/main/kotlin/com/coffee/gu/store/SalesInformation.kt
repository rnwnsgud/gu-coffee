package com.coffee.gu.store

@JvmRecord
data class SalesInformation(
    val location: StoreLocation,
    val hours: List<SalesHour> = emptyList(),
    val phoneNumber: String,
)
