package com.coffee.gu.menu

data class MenuDetailResult(
    val menu: Menu,
    val optionGroups: List<OptionGroup> = emptyList(),
    val options: List<Option> = emptyList(),
)
