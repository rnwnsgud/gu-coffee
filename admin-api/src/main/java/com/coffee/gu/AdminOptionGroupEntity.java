package com.coffee.gu;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "option_group")
@Entity
public class AdminOptionGroupEntity extends AdminBaseEntity{

    private String name;
    private Boolean isExclusive;
    private Boolean isRequired;

    protected AdminOptionGroupEntity() {}

    public AdminOptionGroupEntity(String name, Boolean isExclusive, Boolean isRequired) {
        this.name = name;
        this.isExclusive = isExclusive;
        this.isRequired = isRequired;
    }

    public String getName() {
        return name;
    }

    public Boolean getExclusive() {
        return isExclusive;
    }

    public Boolean getRequired() {
        return isRequired;
    }
}
