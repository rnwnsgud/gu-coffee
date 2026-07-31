package com.coffee.gu.support.event;

import com.coffee.gu.EventDispatcher;
import com.coffee.gu.EventRecoveryScheduler;
import com.coffee.gu.enums.EventLogTarget;
import com.coffee.gu.EventLog;
import com.coffee.gu.EventLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InternalEventRecoveryScheduler implements EventRecoveryScheduler {

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
            eventDispatchers.forEach(eventDispatcher -> eventDispatcher.dispatch(eventLog.getPayload()));
        }
        eventLogRepository.republish(eventLogs.stream().map(EventLog::getEventId).toList());
    }
}
