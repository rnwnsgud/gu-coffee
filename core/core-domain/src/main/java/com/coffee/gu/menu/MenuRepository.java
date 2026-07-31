package com.coffee.gu.menu;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MenuRepository {
    List<Menu> findByCategoryId(Long categoryId, Pageable pageable);
    List<Menu> findByCategoryId(Long categoryId, LocalDateTime cursor, Long lastId, int pageSize);
    Optional<Menu> findById(Long menuId);
    List<Menu> findAllByIdIn(List<Long> menuIds);
}
