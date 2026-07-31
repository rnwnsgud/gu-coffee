package com.coffee.gu;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    public Long getId() { return id; }

}
