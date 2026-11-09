package com.coffee.gu.menu

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class LimitedMenuRepositoryImpl(
    private val limitedMenuJpaRepository: LimitedMenuJpaRepository,
    private val menuJpaRepository: MenuJpaRepository,
) : LimitedMenuRepository {

    override fun findByMenuId(menuId: Long): LimitedMenu? {
        val limitedMenu = limitedMenuJpaRepository.findByMenuId(menuId) ?: return null
        val menu = menuJpaRepository.findByIdOrNull(limitedMenu.menuId)?.toModel() ?: return null
        return limitedMenu.toModel(menu)
    }

    override fun findByMenuIdWithLock(menuId: Long): LimitedMenu? {
        val limitedMenu = limitedMenuJpaRepository.findByMenuIdForUpdate(menuId) ?: return null
        val menu = menuJpaRepository.findByIdOrNull(limitedMenu.menuId)?.toModel() ?: return null
        return limitedMenu.toModel(menu)
    }

    override fun findAllPolicies(): List<LimitedMenuPolicy> {
        return limitedMenuJpaRepository.findAll().map { it.toPolicy() }
    }

    override fun save(limitedMenu: LimitedMenu): LimitedMenu {
        val savedLimitedMenu = limitedMenuJpaRepository.save(LimitedMenuEntity.from(limitedMenu))
        return savedLimitedMenu.toModel(limitedMenu.menu)
    }
}
