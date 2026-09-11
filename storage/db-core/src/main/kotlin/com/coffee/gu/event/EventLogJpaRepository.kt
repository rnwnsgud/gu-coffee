package com.coffee.gu.event

import org.springframework.data.jpa.repository.JpaRepository

interface EventLogJpaRepository : JpaRepository<EventLogEntity, String>
