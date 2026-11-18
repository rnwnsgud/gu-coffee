package com.coffee.gu.order

interface ExternalOrderCreator {
    fun create(newOrder: NewOrder): String
}
