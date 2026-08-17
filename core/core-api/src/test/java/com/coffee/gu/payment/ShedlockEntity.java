package com.coffee.gu.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "shedlock")
public class ShedlockEntity {

    @Id
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "lock_until", nullable = false)
    private Instant lockUntil;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;

    public ShedlockEntity() {
    }

    public ShedlockEntity(String name, Instant lockUntil, Instant lockedAt, String lockedBy) {
        this.name = name;
        this.lockUntil = lockUntil;
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
    }

    public String getName() {
        return name;
    }

    public Instant getLockUntil() {
        return lockUntil;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }
}
