package com.coffee.gu.cancel;

import com.coffee.gu.CancelEvent;
import com.coffee.gu.EventLogRepository;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelTxHandler {

    private final CancelRollbacker cancelRollbacker;
    private final CancelRecorder cancelRecorder;
    private final EventLogRepository eventLogRepository;

    public CancelTxHandler(CancelRollbacker cancelRollbacker, CancelRecorder cancelRecorder, EventLogRepository eventLogRepository) {
        this.cancelRollbacker = cancelRollbacker;
        this.cancelRecorder = cancelRecorder;
        this.eventLogRepository = eventLogRepository;
    }

    @Transactional
    public void completeCancelTx(Order order, Payment payment, CancelEvent event) {
        cancelRollbacker.rollback(order, payment);
        cancelRecorder.record(payment, order);
        eventLogRepository.publish(event);
    }
}
