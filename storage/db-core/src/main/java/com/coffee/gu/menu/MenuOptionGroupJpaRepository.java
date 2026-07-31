package com.coffee.gu.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuOptionGroupJpaRepository extends JpaRepository<MenuOptionGroupEntity, Long> {
    List<MenuOptionGroupEntity> findByMenuId(Long menuId);
}
