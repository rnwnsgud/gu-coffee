package com.coffee.gu.menu;

import java.util.List;

public interface MenuCategoryRepository {
    List<MenuCategory> findAllByMenuIdIn(List<Long> menuIds);
}
