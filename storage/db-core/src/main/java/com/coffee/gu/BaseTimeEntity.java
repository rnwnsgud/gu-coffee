package com.coffee.gu;

import com.coffee.gu.enums.EntityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseTimeEntity {

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isActive() { return this.entityStatus == EntityStatus.ACTIVE; }
    public void active() { this.entityStatus = EntityStatus.ACTIVE; }
    public void delete() { this.entityStatus = EntityStatus.DELETED; }
    public boolean isDeleted() { return this.entityStatus == EntityStatus.DELETED; }
}
