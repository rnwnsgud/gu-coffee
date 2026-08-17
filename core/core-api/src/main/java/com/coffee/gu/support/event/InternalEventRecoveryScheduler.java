package com.coffee.gu.support.event;

import com.coffee.gu.EventDispatcher;
import com.coffee.gu.EventRecoveryScheduler;
import com.coffee.gu.enums.EventLogTarget;
import com.coffee.gu.EventLog;
import com.coffee.gu.EventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InternalEventRecoveryScheduler implements EventRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(InternalEventRecoveryScheduler.class);
    private static final int MAX_RETRY_LIMIT = 5;

    private final EventLogRepository eventLogRepository;
    private final List<EventDispatcher> eventDispatchers;

    public InternalEventRecoveryScheduler(EventLogRepository eventLogRepository, List<EventDispatcher> eventDispatchers) {
        this.eventLogRepository = eventLogRepository;
        this.eventDispatchers = eventDispatchers;
    }

    @Scheduled(fixedRate = 5000)
    public void republish() {
        List<EventLog> eventLogs = eventLogRepository.getUnpublishedEventLogs(EventLogTarget.INTERNAL);
        for (EventLog eventLog : eventLogs) {
            if (eventLog.getRetryCount() != null && eventLog.getRetryCount() >= MAX_RETRY_LIMIT) {
                log.warn("Outbox 이벤트 재발행 5회 초과 실패로 DEAD 전환: eventId={}, eventType={}", eventLog.getEventId(), eventLog.getEventType());
                eventLogRepository.markAsDead(eventLog.getEventId());
                // 알림호출
                continue;
            }

            try {
                eventDispatchers.stream()
                        .filter(eventDispatcher -> eventDispatcher.supports(eventLog.getEventType()))
                        .forEach(eventDispatcher -> eventDispatcher.dispatch(eventLog.getPayload()));

                eventLogRepository.republish(List.of(eventLog.getEventId()));
            } catch (Exception e) {
                log.error("Outbox 이벤트 재발행 실패 (retryCount 증가): eventId={}", eventLog.getEventId(), e);
                eventLogRepository.increaseRetryCount(eventLog.getEventId());
            }
        }
    }
}
