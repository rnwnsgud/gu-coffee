package com.coffee.gu.event;

import org.springframework.data.jpa.repository.JpaRepository;


public interface EventLogJpaRepository extends JpaRepository<EventLogEntity, String> {
}
