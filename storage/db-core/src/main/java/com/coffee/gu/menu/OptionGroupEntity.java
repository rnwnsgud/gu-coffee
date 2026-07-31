package com.coffee.gu.menu;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "option_group")
@Entity
public class OptionGroupEntity extends BaseEntity {

    private String name;
    private Boolean isExclusive;
    private Boolean isRequired;

    protected OptionGroupEntity() {}

    public OptionGroupEntity(String name, Boolean isExclusive, Boolean isRequired) {
        this.name = name;
        this.isExclusive = isExclusive;
        this.isRequired = isRequired;
    }

    public OptionGroup toModel() {
        return new OptionGroup(
                id,
                name,
                isExclusive,
                isRequired
        );
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
