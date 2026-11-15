package com.coffee.gu.menu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MenuAvailabilityReaderTest {

    @InjectMocks
    private lateinit var menuAvailabilityReader: MenuAvailabilityReader

    @Mock
    private lateinit var menuRecipeRepository: MenuRecipeRepository

    @Mock
    private lateinit var ingredientRepository: IngredientRepository

    @Test
    @DisplayName("조회할 메뉴 ID 목록이 비어있으면 빈 Set을 반환한다")
    fun returns_empty_set_when_menu_ids_empty() {
        val result = menuAvailabilityReader.findSoldOutMenuIds(1L, emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("메뉴에 매핑된 레시피가 전혀 없으면 품절 메뉴가 없는 것으로 판단한다")
    fun returns_empty_set_when_no_recipes() {
        given(menuRecipeRepository.findAllByMenuIdIn(listOf(1L, 2L))).willReturn(emptyList())

        val result = menuAvailabilityReader.findSoldOutMenuIds(1L, listOf(1L, 2L))
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("모든 레시피의 원자재가 판매 가능(isAvailable=true)하면 품절 메뉴가 없다")
    fun returns_empty_set_when_all_ingredients_available() {
        // given: 메뉴 1 (원두 1개), 메뉴 2 (원두 1개, 우유 1개)
        val recipe1 = MenuRecipe(1L, menuId = 1L, ingredientId = 10L, requiredQuantity = 1)
        val recipe2 = MenuRecipe(2L, menuId = 2L, ingredientId = 10L, requiredQuantity = 1)
        val recipe3 = MenuRecipe(3L, menuId = 2L, ingredientId = 20L, requiredQuantity = 1)
        given(menuRecipeRepository.findAllByMenuIdIn(listOf(1L, 2L))).willReturn(listOf(recipe1, recipe2, recipe3))

        val bean = Ingredient(id = 1L, storeId = 1L, ingredientId = 10L, name = "원두", isAvailable = true)
        val milk = Ingredient(id = 2L, storeId = 1L, ingredientId = 20L, name = "우유", isAvailable = true)
        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(1L, setOf(10L, 20L))).willReturn(listOf(bean, milk))

        // when
        val result = menuAvailabilityReader.findSoldOutMenuIds(1L, listOf(1L, 2L))

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("원자재가 isAvailable=false인 경우 해당 원자재를 사용하는 메뉴를 품절로 판정한다")
    fun returns_sold_out_menu_ids_when_ingredient_unavailable() {
        // given: 메뉴 1 (원두 1개), 메뉴 2 (우유 1개), 메뉴 3 (시럽 1개)
        val recipe1 = MenuRecipe(1L, menuId = 1L, ingredientId = 10L, requiredQuantity = 1)
        val recipe2 = MenuRecipe(2L, menuId = 2L, ingredientId = 20L, requiredQuantity = 1)
        val recipe3 = MenuRecipe(3L, menuId = 3L, ingredientId = 30L, requiredQuantity = 1)
        given(menuRecipeRepository.findAllByMenuIdIn(listOf(1L, 2L, 3L))).willReturn(listOf(recipe1, recipe2, recipe3))

        val bean = Ingredient(id = 1L, storeId = 1L, ingredientId = 10L, name = "원두", isAvailable = true)
        val milk = Ingredient(id = 2L, storeId = 1L, ingredientId = 20L, name = "우유", isAvailable = false)
        val syrup = Ingredient(id = 3L, storeId = 1L, ingredientId = 30L, name = "바닐라시럽", isAvailable = false)

        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(1L, setOf(10L, 20L, 30L)))
            .willReturn(listOf(bean, milk, syrup))

        // when
        val result = menuAvailabilityReader.findSoldOutMenuIds(1L, listOf(1L, 2L, 3L))

        // then: 메뉴 2 (우유 isAvailable=false)와 메뉴 3 (시럽 isAvailable=false)이 품절로 반환
        assertThat(result).containsExactlyInAnyOrder(2L, 3L)
    }

    @Test
    @DisplayName("매장에 원자재 마스터 자체가 등록되어 있지 않은 경우 품절로 판정한다")
    fun returns_sold_out_when_ingredient_not_registered_in_store() {
        val recipe1 = MenuRecipe(1L, menuId = 1L, ingredientId = 99L, requiredQuantity = 1)
        given(menuRecipeRepository.findAllByMenuIdIn(listOf(1L))).willReturn(listOf(recipe1))
        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(1L, setOf(99L))).willReturn(emptyList())

        val result = menuAvailabilityReader.findSoldOutMenuIds(1L, listOf(1L))

        assertThat(result).containsExactly(1L)
    }
}
