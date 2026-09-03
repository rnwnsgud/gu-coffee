package com.coffee.gu.store

class SalesInformation(
    val location: StoreLocation,
    val hours: List<SalesHour> = emptyList(),
    val phoneNumber: String,
)
