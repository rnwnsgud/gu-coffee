package com.coffee.gu

import com.coffee.gu.enums.EntityStatus
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    var entityStatus: EntityStatus = EntityStatus.ACTIVE

    @CreatedDate
    @Column(updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    val isActive: Boolean
        get() = this.entityStatus == EntityStatus.ACTIVE

    val isDeleted: Boolean
        get() = this.entityStatus == EntityStatus.DELETED

    fun active() {
        this.entityStatus = EntityStatus.ACTIVE
    }

    fun delete() {
        this.entityStatus = EntityStatus.DELETED
    }
}
