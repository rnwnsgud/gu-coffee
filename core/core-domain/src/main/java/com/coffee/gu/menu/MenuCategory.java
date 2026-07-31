package com.coffee.gu.menu;

public class MenuCategory {
    private Long id;
    private Long menuId;
    private Long categoryId;

    public MenuCategory(Long id, Long menuId, Long categoryId) {
        this.id = id;
        this.menuId = menuId;
        this.categoryId = categoryId;
    }

    public Long getId() {
        return id;
    }

    public Long getMenuId() {
        return menuId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}
