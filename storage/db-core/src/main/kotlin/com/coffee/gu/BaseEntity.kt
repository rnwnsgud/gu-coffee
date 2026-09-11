package com.coffee.gu

import com.coffee.gu.enums.EntityStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    var entityStatus: EntityStatus = EntityStatus.ACTIVE

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.MIN

    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.MIN

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
