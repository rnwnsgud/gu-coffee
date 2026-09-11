package com.coffee.gu.menu

interface OptionRepository {
    fun findAllByIdOptionGroupIdIn(optionGroupIds: List<Long>): List<Option>
}
