package com.coffee.gu.menu

interface LimitedMenuRepository {
    fun findByMenuId(menuId: Long): LimitedMenu?
    fun findByMenuIdWithLock(menuId: Long): LimitedMenu?
    fun findAllPolicies(): List<LimitedMenuPolicy>
    fun save(limitedMenu: LimitedMenu): LimitedMenu
}
