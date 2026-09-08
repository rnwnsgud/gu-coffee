package com.coffee.gu

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable

@MappedSuperclass
abstract class BaseCustomIdEntity<T : Any>(
    @Transient
    var isNewEntity: Boolean = true,
) : BaseEntity(), Persistable<T> {

    abstract override fun getId(): T

    override fun isNew(): Boolean = this.isNewEntity

    @PostLoad
    @PostPersist
    protected fun markNotNew() {
        this.isNewEntity = false
    }
}
