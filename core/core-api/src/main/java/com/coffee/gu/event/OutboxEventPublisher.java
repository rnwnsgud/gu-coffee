package com.coffee.gu.event;

import com.coffee.gu.Event;
import com.coffee.gu.EventLogRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {

    private final EventLogRepository eventLogRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OutboxEventPublisher(EventLogRepository eventLogRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.eventLogRepository = eventLogRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * DB Outbox에 이벤트를 유실 없이 원자적으로 저장(saveIfNotExists)하고,
     * 실시간 수신자(Listener)에게 스프링 이벤트를 발행(publishEvent)한다.
     */
    public boolean publishOutboxEvent(Event event) {
        boolean saved = eventLogRepository.saveIfNotExists(event);
        if (saved) {
            applicationEventPublisher.publishEvent(event);
        }
        return saved;
    }
}
