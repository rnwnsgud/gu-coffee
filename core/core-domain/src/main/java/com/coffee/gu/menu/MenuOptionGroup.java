package com.coffee.gu.menu;

public class MenuOptionGroup {
    private Long menuId;
    private Long optionGroupId;

    public MenuOptionGroup(Long menuId, Long optionGroupId) {
        this.menuId = menuId;
        this.optionGroupId = optionGroupId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public Long getOptionGroupId() {
        return optionGroupId;
    }
}
