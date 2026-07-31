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
    private AdminEntityStatus status = AdminEntityStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime created = LocalDateTime.MIN;

    @LastModifiedDate
    private LocalDateTime updatedAt = LocalDateTime.MIN;

    public Long getId() { return id; }

    void active() {
        this.status = AdminEntityStatus.ACTIVE;
    }

    boolean isActive() {
        return this.status == AdminEntityStatus.ACTIVE;
    }

    void delete() {
        this.status = AdminEntityStatus.DELETED;
    }

    boolean isDeleted() {
        return this.status == AdminEntityStatus.DELETED;
    }

}
