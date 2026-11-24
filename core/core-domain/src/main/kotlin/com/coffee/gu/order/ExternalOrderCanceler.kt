package com.coffee.gu.order

interface ExternalOrderCanceler {
    fun cancel(order: Order)
}
