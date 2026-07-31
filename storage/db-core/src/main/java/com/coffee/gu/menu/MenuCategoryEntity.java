package com.coffee.gu.menu;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "menu_category")
@Entity
public class MenuCategoryEntity extends BaseEntity {
    private Long menuId;
    private Long categoryId;

    protected MenuCategoryEntity() {}

    public MenuCategoryEntity(Long menuId, Long categoryId) {
        this.menuId = menuId;
        this.categoryId = categoryId;
    }

    public MenuCategory toModel() {
        return new MenuCategory(id, menuId, categoryId);
    }

    public Long getMenuId() {return menuId;}

    public Long getCategoryId() {
        return categoryId;
    }
}
