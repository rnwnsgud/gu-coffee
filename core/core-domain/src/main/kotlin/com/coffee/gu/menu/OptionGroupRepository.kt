package com.coffee.gu.menu

interface OptionGroupRepository {
    fun findAllById(ids: List<Long>): List<OptionGroup>
}
