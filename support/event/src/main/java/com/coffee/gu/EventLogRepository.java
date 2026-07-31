package com.coffee.gu;

import com.coffee.gu.enums.EventLogTarget;

import java.util.Collection;
import java.util.List;

public interface EventLogRepository {
    boolean saveUniqueEvent(Event event);

    /**
     * 반드시 기존 비즈니스 트랜잭션 내부에서 호출되어야 합니다. @Transactional(propagation = Propagation.MANDATORY)
     */
    void publish(Event event);
    void republish(Collection<String> eventIds);
    List<EventLog> getUnpublishedEventLogs(EventLogTarget eventLogTarget);
}
