package com.coffee.gu.menu;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.OffsetLimit;
import com.coffee.gu.Page;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MenuFinder {

    private final MenuRepository menuRepository;

    public MenuFinder(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public Page<Menu> findByCategory(Long categoryId, OffsetLimit offsetLimit) {
        int pageSize = offsetLimit.limit();
        List<Menu> menus = menuRepository.findByCategoryId(categoryId, offsetLimit.withLimit(pageSize + 1));
        return Page.of(menus, pageSize);
    }

    public Page<Menu> findByCategory(Long categoryId, int pageSize, LocalDateTime cursor, Long lastId) {
        List<Menu> menus = menuRepository.findByCategoryId(categoryId, cursor, lastId, pageSize);
        return Page.of(menus, pageSize);
    }

    public Menu getById(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
    }

    public List<Menu> findAllByIdIn(List<Long> menuIds) {
        return menuRepository.findAllByIdIn(menuIds);
    }

}
