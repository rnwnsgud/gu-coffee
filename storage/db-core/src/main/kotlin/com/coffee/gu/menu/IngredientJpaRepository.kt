package com.coffee.gu.menu

import org.springframework.data.jpa.repository.JpaRepository

interface IngredientJpaRepository : JpaRepository<IngredientEntity, Long> {
    fun findByStoreIdAndIngredientId(storeId: Long, ingredientId: Long): IngredientEntity?
    fun findAllByStoreId(storeId: Long): List<IngredientEntity>
    fun findAllByStoreIdAndIngredientIdIn(storeId: Long, ingredientIds: Collection<Long>): List<IngredientEntity>
}
