package com.coffee.gu.menu

interface IngredientRepository {
    fun findByStoreIdAndIngredientId(storeId: Long, ingredientId: Long): Ingredient?
    fun findAllByStoreId(storeId: Long): List<Ingredient>
    fun findAllByStoreIdAndIngredientIdIn(storeId: Long, ingredientIds: Collection<Long>): List<Ingredient>
    fun save(ingredient: Ingredient): Ingredient
    fun saveAll(ingredients: Collection<Ingredient>): List<Ingredient>
}
