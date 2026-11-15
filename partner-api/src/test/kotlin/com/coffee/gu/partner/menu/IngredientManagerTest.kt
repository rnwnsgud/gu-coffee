package com.coffee.gu.partner.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.menu.IngredientRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
class IngredientManagerTest {

    @InjectMocks
    private lateinit var ingredientManager: IngredientManager

    @Mock
    private lateinit var ingredientRepository: IngredientRepository

    @Test
    @DisplayName("존재하지 않는 매장 원자재의 상태를 변경하려 하면 NOT_FOUND_DATA 예외가 발생한다")
    fun throwNotFoundWhenIngredientDoesNotExist() {
        given(ingredientRepository.findByStoreIdAndIngredientId(1L, 999L)).willReturn(null)

        assertThatThrownBy {
            ingredientManager.updateAvailability(1L, 999L, false)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_DATA)
    }

    @Test
    @DisplayName("원자재 가용 상태를 변경하고 저장된 도메인 모델을 반환한다")
    fun updateAvailabilitySuccessfully() {
        val ingredient = Ingredient(id = 1L, storeId = 1L, ingredientId = 200L, name = "우유", isAvailable = true)
        given(ingredientRepository.findByStoreIdAndIngredientId(1L, 200L)).willReturn(ingredient)
        given(ingredientRepository.save(any())).willAnswer { it.getArgument(0) }

        val updated = ingredientManager.updateAvailability(1L, 200L, false)

        assertThat(updated.isAvailable).isFalse()
    }
}
