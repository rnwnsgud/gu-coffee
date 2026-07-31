package com.coffee.gu;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@MappedSuperclass
public abstract class BaseCustomIdEntity<T> extends BaseTimeEntity implements Persistable<T> {

    @Id
    protected T id;
    @Transient
    private boolean isNewEntity = true;

    protected BaseCustomIdEntity() {}

    protected BaseCustomIdEntity(T id, boolean isNewEntity) {
        this.id = id;
        this.isNewEntity = isNewEntity;
    }

    @Override
    public T getId() {
        return this.id;
    }

    @Override
    public boolean isNew() {
        return this.isNewEntity;
    }

    @PostLoad
    @PostPersist
    protected void markNotNew() {
        this.isNewEntity = false;
    }
}
