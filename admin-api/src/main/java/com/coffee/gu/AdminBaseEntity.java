package com.coffee.gu;

import com.coffee.admin.domain.AdminEntityStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
abstract class AdminBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 0L;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private AdminEntityStatus entityStatus = AdminEntityStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.MIN;

    @LastModifiedDate
    private LocalDateTime updatedAt = LocalDateTime.MIN;

    public Long getId() { return id; }

    void active() {
        this.entityStatus = AdminEntityStatus.ACTIVE;
    }

    boolean isActive() {
        return this.entityStatus == AdminEntityStatus.ACTIVE;
    }

    void delete() {
        this.entityStatus = AdminEntityStatus.DELETED;
    }

    boolean isDeleted() {
        return this.entityStatus == AdminEntityStatus.DELETED;
    }

}
