package com.coffee.gu.menu;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "menu_option_group")
@Entity
public class MenuOptionGroupEntity extends BaseEntity {

    private Long menuId;
    private Long optionGroupId;

    public MenuOptionGroupEntity() {}

    public MenuOptionGroupEntity(Long menuId, Long optionGroupId) {
        this.menuId = menuId;
        this.optionGroupId = optionGroupId;
    }

    public  MenuOptionGroup toModel() {
        return new MenuOptionGroup(
                menuId,
                optionGroupId
        );
    }

    public Long getMenuId() {
        return menuId;
    }

    public Long getOptionGroupId() {
        return optionGroupId;
    }
}
