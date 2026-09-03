package com.coffee.gu.store

interface SalesHourRepository {
    fun findAllByStores(stores: Collection<Store>): List<SalesHour>
}
