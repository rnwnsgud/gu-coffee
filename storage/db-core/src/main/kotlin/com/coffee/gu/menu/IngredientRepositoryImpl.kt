package com.coffee.gu.menu

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class IngredientRepositoryImpl(
    private val ingredientJpaRepository: IngredientJpaRepository,
) : IngredientRepository {

    override fun findById(id: Long): Ingredient? {
        return ingredientJpaRepository.findByIdOrNull(id)?.toModel()
    }

    override fun findByStoreIdAndIngredientId(storeId: Long, ingredientId: Long): Ingredient? {
        return ingredientJpaRepository.findByStoreIdAndIngredientId(storeId, ingredientId)?.toModel()
    }

    override fun findAllByStoreId(storeId: Long): List<Ingredient> {
        return ingredientJpaRepository.findAllByStoreId(storeId).map { it.toModel() }
    }

    override fun findAllByStoreIdAndIngredientIdIn(
        storeId: Long,
        ingredientIds: Collection<Long>,
    ): List<Ingredient> {
        if (ingredientIds.isEmpty()) return emptyList()
        return ingredientJpaRepository.findAllByStoreIdAndIngredientIdIn(storeId, ingredientIds)
            .map { it.toModel() }
    }

    override fun save(ingredient: Ingredient): Ingredient {
        val savedIngredient = ingredientJpaRepository.save(IngredientEntity.from(ingredient))
        return savedIngredient.toModel()
    }

    override fun saveAll(ingredients: Collection<Ingredient>): List<Ingredient> {
        if (ingredients.isEmpty()) return emptyList()
        val savedIngredients = ingredientJpaRepository.saveAll(ingredients.map { IngredientEntity.from(it) })
        return savedIngredients.map { it.toModel() }
    }
}
