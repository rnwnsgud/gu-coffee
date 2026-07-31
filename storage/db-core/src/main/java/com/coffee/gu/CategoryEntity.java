package com.coffee.gu;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "category")
@Entity
public class CategoryEntity extends BaseEntity{
    private String name;
}
