package com.coffee.gu;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Table(name = "option")
@Entity
public class AdminOptionEntity extends AdminBaseEntity{

    private Long optionGroupId;
    private String name;
    private BigDecimal extraPrice;

    protected AdminOptionEntity() {}

    public AdminOptionEntity(Long optionGroupId, String name, BigDecimal extraPrice) {
        this.optionGroupId = optionGroupId;
        this.name = name;
        this.extraPrice = extraPrice;
    }
}
