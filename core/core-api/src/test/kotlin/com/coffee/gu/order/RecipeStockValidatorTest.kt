package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.menu.IngredientRepository
import com.coffee.gu.menu.MenuRecipe
import com.coffee.gu.menu.MenuRecipeRepository
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock

class RecipeStockValidatorTest {

    private val menuRecipeRepository: MenuRecipeRepository = mock()
    private val ingredientRepository: IngredientRepository = mock()
    private val validator = RecipeStockValidator(menuRecipeRepository, ingredientRepository)

    @Test
    @DisplayName("특정 매장의 원자재가 수동 품절(isAvailable=false)이면 해당 매장의 단품과 세트메뉴 주문은 차단되고 타 매장은 정상 통과한다")
    fun blockSingleAndSetMenuOnlyForSoldOutStore() {
        // Given
        val gangnamStoreId = 1L
        val hongdaeStoreId = 2L
        val latteMenuId = 10L
        val morningSetMenuId = 20L
        val milkIngredientId = 100L

        val gangnamMilkUnavailable = Ingredient(
            id = 1,
            storeId = gangnamStoreId,
            ingredientId = milkIngredientId,
            name = "우유",
            isAvailable = false, // 강남점 점주 수동 품절 처리
        )
        val hongdaeMilkAvailable = Ingredient(
            id = 2,
            storeId = hongdaeStoreId,
            ingredientId = milkIngredientId,
            name = "우유",
            isAvailable = true, // 홍대점은 정상 판매 중
        )

        given(menuRecipeRepository.findAllByMenuIdIn(setOf(latteMenuId)))
            .willReturn(listOf(MenuRecipe(id = 1, menuId = latteMenuId, ingredientId = milkIngredientId, requiredQuantity = 1)))
        given(menuRecipeRepository.findAllByMenuIdIn(setOf(morningSetMenuId)))
            .willReturn(listOf(MenuRecipe(id = 2, menuId = morningSetMenuId, ingredientId = milkIngredientId, requiredQuantity = 1)))

        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(gangnamStoreId, setOf(milkIngredientId)))
            .willReturn(listOf(gangnamMilkUnavailable))
        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(hongdaeStoreId, setOf(milkIngredientId)))
            .willReturn(listOf(hongdaeMilkAvailable))

        // When & Then - 강남점 단품 라떼 주문 차단
        assertThatThrownBy {
            validator.validateAvailability(gangnamStoreId, setOf(latteMenuId))
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // When & Then - 강남점 우유 포함 세트메뉴 주문도 차단
        assertThatThrownBy {
            validator.validateAvailability(gangnamStoreId, setOf(morningSetMenuId))
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // When & Then - 홍대점 세트메뉴 주문은 정상 통과
        assertThatCode {
            validator.validateAvailability(hongdaeStoreId, setOf(morningSetMenuId))
        }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("세트메뉴 구성품 중 일부 원자재(베이글)만 품절인 경우, 단품(라떼)은 통과하지만 세트메뉴는 차단된다")
    fun blockSetMenuWhenSpecificIngredientOutOfStock() {
        // Given
        val storeId = 1L
        val latteMenuId = 10L
        val morningSetMenuId = 20L
        val milkIngredientId = 100L
        val bagelIngredientId = 200L

        val milk = Ingredient(id = 1, storeId = storeId, ingredientId = milkIngredientId, name = "우유", isAvailable = true)
        val bagelOutOfStock = Ingredient(id = 2, storeId = storeId, ingredientId = bagelIngredientId, name = "베이글", isAvailable = false)

        given(menuRecipeRepository.findAllByMenuIdIn(setOf(latteMenuId)))
            .willReturn(listOf(MenuRecipe(id = 1, menuId = latteMenuId, ingredientId = milkIngredientId, requiredQuantity = 1)))
        given(menuRecipeRepository.findAllByMenuIdIn(setOf(morningSetMenuId)))
            .willReturn(
                listOf(
                    MenuRecipe(id = 2, menuId = morningSetMenuId, ingredientId = milkIngredientId, requiredQuantity = 1),
                    MenuRecipe(id = 3, menuId = morningSetMenuId, ingredientId = bagelIngredientId, requiredQuantity = 1),
                ),
            )
        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(storeId, setOf(milkIngredientId)))
            .willReturn(listOf(milk))
        given(ingredientRepository.findAllByStoreIdAndIngredientIdIn(storeId, setOf(milkIngredientId, bagelIngredientId)))
            .willReturn(listOf(milk, bagelOutOfStock))

        // When & Then - 라떼는 베이글을 필요로 하지 않으므로 정상 통과
        assertThatCode {
            validator.validateAvailability(storeId, setOf(latteMenuId))
        }.doesNotThrowAnyException()

        // When & Then - 모닝세트는 베이글이 품절이므로 차단
        assertThatThrownBy {
            validator.validateAvailability(storeId, setOf(morningSetMenuId))
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)
    }

    @Test
    @DisplayName("레시피 정보가 없는 일반 메뉴이거나 원자재가 가용 상태이면 검증을 정상 통과한다")
    fun passWhenNoRecipeOrIngredientIsAvailable() {
        // Given
        val storeId = 1L
        val americanoMenuId = 99L
        given(menuRecipeRepository.findAllByMenuIdIn(setOf(americanoMenuId)))
            .willReturn(emptyList())

        // When & Then
        assertThatCode {
            validator.validateAvailability(storeId, setOf(americanoMenuId))
        }.doesNotThrowAnyException()
    }
}
