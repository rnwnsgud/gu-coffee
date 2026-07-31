package com.coffee.gu.menu;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Table(name = "option")
@Entity
public class OptionEntity extends BaseEntity {

    private Long optionGroupId;
    private String name;
    private BigDecimal extraPrice;

    public OptionEntity() {}

    public OptionEntity(Long optionGroupId, String name, BigDecimal extraPrice) {
        this.optionGroupId = optionGroupId;
        this.name = name;
        this.extraPrice = extraPrice;
    }

    public Option toModel() {
        return new Option(id, optionGroupId, name, extraPrice);
    }

    public Long getOptionGroupId() {
        return optionGroupId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getExtraPrice() {
        return extraPrice;
    }
}
