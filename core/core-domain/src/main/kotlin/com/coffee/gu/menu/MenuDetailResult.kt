package com.coffee.gu.menu

class MenuDetailResult(
    val menu: Menu,
    val optionGroups: List<OptionGroup> = emptyList(),
    val options: List<Option> = emptyList(),
)
