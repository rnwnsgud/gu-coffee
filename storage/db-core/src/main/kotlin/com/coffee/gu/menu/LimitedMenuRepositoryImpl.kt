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

    override fun findAllByMenuIdIn(menuIds: Collection<Long>): List<LimitedMenu> {
        if (menuIds.isEmpty()) return emptyList()
        val limitedMenus = limitedMenuJpaRepository.findAllByMenuIdIn(menuIds)
        if (limitedMenus.isEmpty()) return emptyList()
        val menusById = menuJpaRepository.findAllById(limitedMenus.map { it.menuId })
            .associate { it.id to it.toModel() }
        return limitedMenus.mapNotNull { limitedMenu ->
            val menu = menusById[limitedMenu.menuId] ?: return@mapNotNull null
            limitedMenu.toModel(menu)
        }
    }

    override fun findAllMenuIds(): List<Long> {
        return limitedMenuJpaRepository.findAllMenuIds()
    }

    override fun save(limitedMenu: LimitedMenu): LimitedMenu {
        val savedLimitedMenu = limitedMenuJpaRepository.save(LimitedMenuEntity.from(limitedMenu))
        return savedLimitedMenu.toModel(limitedMenu.menu)
    }
}
