package com.coffee.gu.store

import com.coffee.gu.enums.StoreStatus

class Store @JvmOverloads constructor(
    val id: Long = 0,
    val name: String,
    val branchCode: String,
    val status: StoreStatus,
    var salesInformation: SalesInformation? = null,
    val businessInformation: BusinessInformation? = null,
) {
    fun fillSalesInformation(salesInformation: SalesInformation) {
        this.salesInformation = salesInformation
    }
}
