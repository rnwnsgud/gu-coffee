package com.coffee.gu.menu;

import java.util.List;

public interface MenuOptionGroupRepository {
    List<MenuOptionGroup> findByMenuId(Long menuId);
}
