package com.coffee.gu.cancel;

import com.coffee.gu.CancelEvent;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderReader;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CancelEventListener {

    private final CancelService cancelService;
    private final OrderReader orderReader;

    public CancelEventListener(CancelService cancelService, OrderReader orderReader) {
        this.cancelService = cancelService;
        this.orderReader = orderReader;
    }

    @EventListener
    public void handle(CancelEvent event) {
        Order order = orderReader.getByOrderKey(event.getOrderKey());
        cancelService.cancel(order, event);
    }
}
