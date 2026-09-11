package com.coffee.gu.cancel

import com.coffee.gu.CancelEvent
import com.coffee.gu.order.OrderReader
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CancelEventListener(
    private val cancelService: CancelService,
    private val orderReader: OrderReader,
) {
    @EventListener
    fun handle(event: CancelEvent) {
        val order = orderReader.getByOrderKey(event.orderKey)
        cancelService.cancel(order, event)
    }
}
