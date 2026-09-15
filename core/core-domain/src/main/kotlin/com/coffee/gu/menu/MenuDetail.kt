package com.coffee.gu.menu

data class MenuDetail(
    val nutrition: Nutrition,
    val containedAllergens: String? = null,
    val mayContainAllergens: String? = null,
)
