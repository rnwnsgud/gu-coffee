package com.coffee.gu.event;

import com.coffee.gu.Event;
import com.coffee.gu.EventLog;
import com.coffee.gu.EventLogRepository;
import com.coffee.gu.enums.EventLogStatus;
import com.coffee.gu.enums.EventLogTarget;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.coffee.gu.event.QEventLogEntity.eventLogEntity;

@Repository
public class EventLogRepositoryImpl implements EventLogRepository {
    private final EventLogJpaRepository eventLogJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final JsonMapper jsonMapper;

    public EventLogRepositoryImpl(EventLogJpaRepository eventLogJpaRepository, JPAQueryFactory jpaQueryFactory, JsonMapper jsonMapper) {
        this.eventLogJpaRepository = eventLogJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean saveIfNotExists(Event event) {
        try {
            String payload = jsonMapper.writeValueAsString(event);
            eventLogJpaRepository.save(EventLogEntity.create(event.getEventId(), event.getEventType(), payload));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void publish(Event event) {
        jpaQueryFactory
                .update(eventLogEntity)
                .set(eventLogEntity.isPublished, true)
                .set(eventLogEntity.status, EventLogStatus.SUCCESS)
                .set(eventLogEntity.publishedAt, LocalDateTime.now())
                .where(eventLogEntity.id.eq(event.getEventId()))
                .execute();
    }

    @Transactional
    @Override
    public void republish(Collection<String> eventIds) {
        jpaQueryFactory
                .update(eventLogEntity)
                .set(eventLogEntity.isPublished, true)
                .set(eventLogEntity.status, EventLogStatus.SUCCESS)
                .set(eventLogEntity.publishedAt, LocalDateTime.now())
                .where(eventLogEntity.id.in(eventIds))
                .execute();
    }

    @Transactional
    @Override
    public void increaseRetryCount(String eventId) {
        jpaQueryFactory
                .update(eventLogEntity)
                .set(eventLogEntity.retryCount, eventLogEntity.retryCount.add(1))
                .where(eventLogEntity.id.eq(eventId))
                .execute();
    }

    @Transactional
    @Override
    public void markAsDead(String eventId) {
        jpaQueryFactory
                .update(eventLogEntity)
                .set(eventLogEntity.status, EventLogStatus.DEAD)
                .where(eventLogEntity.id.eq(eventId))
                .execute();
    }

    @Override
    public List<EventLog> getUnpublishedEventLogs(EventLogTarget eventLogTarget) {
        LocalDateTime createdBefore = LocalDateTime.now().minusMinutes(5);
        return jpaQueryFactory
                .select(eventLogEntity)
                .from(eventLogEntity)
                .where(eventLogEntity.isPublished.isFalse())
                .where(eventLogEntity.status.ne(EventLogStatus.DEAD).or(eventLogEntity.status.isNull()))
                .where(
                        eventLogEntity.eventLogTarget.eq(eventLogTarget),
                        eventLogEntity.createdAt.before(createdBefore)
                )
                .limit(1000)
                .orderBy(eventLogEntity.createdAt.asc())
                .fetch()
                .stream()
                .map(EventLogEntity::toModel)
                .toList();
    }
}
