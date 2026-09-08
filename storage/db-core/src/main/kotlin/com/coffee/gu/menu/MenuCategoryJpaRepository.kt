package com.coffee.gu.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuCategoryJpaRepository : JpaRepository<MenuCategoryEntity, Long>
