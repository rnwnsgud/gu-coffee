package com.coffee.gu.menu;


import com.coffee.gu.OffsetLimit;
import com.coffee.gu.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MenuService {

    private final MenuFinder menuFinder;
    private final OptionFinder optionFinder;

    public MenuService(MenuFinder menuFinder, OptionFinder optionFinder) {
        this.menuFinder = menuFinder;
        this.optionFinder = optionFinder;
    }

    public Page<Menu> findMenus(Long categoryId, OffsetLimit offsetLimit) {
        return menuFinder.findByCategory(categoryId, offsetLimit);
    }

    public Page<Menu> findMenus(Long categoryId, int pageSize, LocalDateTime cursor, Long lastId) {
        return menuFinder.findByCategory(categoryId, pageSize, cursor, lastId);
    }

    public MenuDetailResult getMenu(Long menuId) {
        Menu menu = menuFinder.getById(menuId);
        List<OptionGroup> optionGroups = optionFinder.findByMenuId(menuId);
        List<Option> options = optionFinder.findByOptionGroups(optionGroups);
        return new MenuDetailResult(menu, optionGroups, options);
    }
}
