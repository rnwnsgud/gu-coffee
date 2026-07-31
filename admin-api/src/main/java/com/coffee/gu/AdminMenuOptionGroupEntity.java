package com.coffee.gu;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "menu_option_group")
@Entity
public class AdminMenuOptionGroupEntity extends AdminBaseEntity{

    private Long menuId;
    private Long optionGroupId;

    protected AdminMenuOptionGroupEntity() {}

    public AdminMenuOptionGroupEntity(Long menuId, Long optionGroupId) {
        this.menuId = menuId;
        this.optionGroupId = optionGroupId;
    }

}
