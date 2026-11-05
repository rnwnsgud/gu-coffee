package com.coffee.gu.menu

interface LimitedMenuRepository {
    fun findByMenuId(menuId: Long): LimitedMenu?
    fun findByMenuIdWithLock(menuId: Long): LimitedMenu?
    fun findAllByMenuIdIn(menuIds: Collection<Long>): List<LimitedMenu>
    fun findAllMenuIds(): List<Long>
    fun save(limitedMenu: LimitedMenu): LimitedMenu
}
