package com.coffee.gu.partner.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.partner.auth.PartnerPrincipal
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PartnerIngredientServiceTest {

    @InjectMocks
    private lateinit var partnerIngredientService: PartnerIngredientService

    @Mock
    private lateinit var ingredientFinder: IngredientFinder

    @Mock
    private lateinit var ingredientManager: IngredientManager

    @Test
    @DisplayName("점주 소속 매장과 요청 storeId가 일치하면 원자재 상태 변경을 정상 수행한다")
    fun updateAvailabilityWhenStoreOwnershipMatches() {
        val owner = PartnerPrincipal(partnerId = "PARTNER-1", storeId = 1L)
        val updated = Ingredient(id = 1L, storeId = 1L, ingredientId = 200L, name = "우유", isAvailable = false)
        given(ingredientManager.updateAvailability(1L, 200L, false)).willReturn(updated)

        val result = partnerIngredientService.updateAvailability(owner, 1L, 200L, false)

        assertThat(result.isAvailable).isFalse()
    }

    @Test
    @DisplayName("점주 소속 매장과 다른 storeId에 대해 조회 또는 수정을 요청하면 FORBIDDEN 예외가 발생하고 하위 계층을 호출하지 않는다")
    fun throwForbiddenWhenStoreOwnershipMismatches() {
        val gangnamOwner = PartnerPrincipal(partnerId = "PARTNER-1", storeId = 1L)
        val hongdaeStoreId = 2L

        assertThatThrownBy {
            partnerIngredientService.getStoreIngredients(gangnamOwner, hongdaeStoreId)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)

        assertThatThrownBy {
            partnerIngredientService.updateAvailability(gangnamOwner, hongdaeStoreId, 200L, false)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)

        then(ingredientFinder).shouldHaveNoInteractions()
        then(ingredientManager).shouldHaveNoInteractions()
    }
}
