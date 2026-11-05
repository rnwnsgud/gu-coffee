package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LimitedMenuManager(
    private val limitedMenuRepository: LimitedMenuRepository,
) {
    @Transactional
    fun create(limitedMenu: LimitedMenu): LimitedMenu {
        return limitedMenuRepository.save(limitedMenu)
    }

    @Transactional
    fun save(limitedMenu: LimitedMenu): LimitedMenu {
        return limitedMenuRepository.save(limitedMenu)
    }

    @Transactional
    fun increaseSoldQuantity(menuId: Long, quantity: Int): LimitedMenu {
        val limitedMenu = limitedMenuRepository.findByMenuIdWithLock(menuId)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val updatedLimitedMenu = limitedMenu.increaseSoldQuantity(quantity)
        return limitedMenuRepository.save(updatedLimitedMenu)
    }

    @Transactional
    fun decreaseSoldQuantity(menuId: Long, quantity: Int): LimitedMenu {
        val limitedMenu = limitedMenuRepository.findByMenuIdWithLock(menuId)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val updatedLimitedMenu = limitedMenu.decreaseSoldQuantity(quantity)
        return limitedMenuRepository.save(updatedLimitedMenu)
    }
}
