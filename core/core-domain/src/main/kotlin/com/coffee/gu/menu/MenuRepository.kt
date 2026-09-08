package com.coffee.gu.menu

import com.coffee.gu.OffsetLimit
import java.time.LocalDateTime
import java.util.Optional

interface MenuRepository {
    fun findByCategoryId(categoryId: Long, offsetLimit: OffsetLimit): List<Menu>
    fun findByCategoryId(categoryId: Long, cursor: LocalDateTime?, lastId: Long?, pageSize: Int): List<Menu>
    fun findById(menuId: Long): Menu
    fun findAllByIdIn(menuIds: List<Long>): List<Menu>
}
