package com.coffee.gu.menu

import org.springframework.stereotype.Component

@Component
class OptionFinder(
    private val menuOptionGroupRepository: MenuOptionGroupRepository,
    private val optionGroupRepository: OptionGroupRepository,
    private val optionRepository: OptionRepository
) {
    fun findByMenuId(menuId: Long): List<OptionGroup> {
        val mappings = menuOptionGroupRepository.findByMenuId(menuId)
        return optionGroupRepository.findAllById(mappings.map { it.optionGroupId })
    }

    fun findByOptionGroups(optionGroups: List<OptionGroup>): List<Option> {
        return optionRepository.findAllByIdOptionGroupIdIn(optionGroups.map { it.id })
    }
}
