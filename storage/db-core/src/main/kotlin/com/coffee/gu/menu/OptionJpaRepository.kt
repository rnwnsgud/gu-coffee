package com.coffee.gu.menu

import org.springframework.data.jpa.repository.JpaRepository

interface OptionJpaRepository : JpaRepository<OptionEntity, Long> {
    fun findAllByOptionGroupIdIn(optionGroupIds: List<Long>): List<OptionEntity>
}
