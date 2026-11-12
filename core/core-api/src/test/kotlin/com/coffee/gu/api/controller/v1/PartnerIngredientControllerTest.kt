package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.api.controller.v1.request.UpdateIngredientAvailabilityRequest
import com.coffee.gu.api.controller.v1.request.UpdateIngredientStockRequest
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.menu.PartnerIngredientService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@MockitoSettings(strictness = Strictness.LENIENT)
class PartnerIngredientControllerTest : RestDocsTest() {

    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var partnerIngredientService: PartnerIngredientService

    @InjectMocks
    private lateinit var partnerIngredientController: PartnerIngredientController

    override val controller: Any
        get() = partnerIngredientController

    @Test
    @DisplayName("매장별 원자재 재고 및 품절 상태 목록 조회 API")
    fun getStoreIngredients() {
        val storeId = 1L
        val ingredients = listOf(
            Ingredient(id = 1L, storeId = storeId, ingredientId = 100L, name = "에스프레소 원두", stockQuantity = 100, isAvailable = true),
            Ingredient(id = 2L, storeId = storeId, ingredientId = 200L, name = "우유", stockQuantity = 50, isAvailable = true),
            Ingredient(id = 3L, storeId = storeId, ingredientId = 400L, name = "플레인 베이글", stockQuantity = 20, isAvailable = true),
        )
        given(partnerIngredientService.getStoreIngredients(storeId)).willReturn(ingredients)

        mockMvc.perform(
            get("/v1/partner/stores/{storeId}/ingredients", storeId)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    identifier = "partner-ingredient-list",
                    tag = "Partner Ingredient",
                    summary = "매장별 원자재 재고 및 품절 상태 목록 조회 API",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("storeId").description("매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data[].storeId").description("매장 식별자"),
                            fieldWithPath("data[].ingredientId").description("원자재 마스터 식별자 (예: 100=원두, 200=우유, 400=베이글)"),
                            fieldWithPath("data[].name").description("원자재 명칭"),
                            fieldWithPath("data[].stockQuantity").description("현재 매장 잔여 재고 수량"),
                            fieldWithPath("data[].isAvailable").description("판매 가용 여부 (false일 경우 수동 품절 상태)"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("매장 특정 원자재 품절 및 판매 가능 상태 변경 API")
    fun updateAvailability() {
        val storeId = 1L
        val ingredientId = 200L
        val request = UpdateIngredientAvailabilityRequest(isAvailable = false)
        val updated = Ingredient(
            id = 2L,
            storeId = storeId,
            ingredientId = ingredientId,
            name = "우유",
            stockQuantity = 50,
            isAvailable = false,
        )
        given(partnerIngredientService.updateAvailability(storeId, ingredientId, false)).willReturn(updated)

        mockMvc.perform(
            patch("/v1/partner/stores/{storeId}/ingredients/{ingredientId}/availability", storeId, ingredientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.isAvailable").value(false))
            .andDo(
                document(
                    identifier = "partner-ingredient-update-availability",
                    tag = "Partner Ingredient",
                    summary = "매장 특정 원자재 품절 및 판매 가능 상태 변경 API",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("storeId").description("매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                            parameterWithName("ingredientId").description("원자재 마스터 식별자 (예: 200=우유)"),
                        ),
                        requestFields(
                            fieldWithPath("isAvailable").description("변경할 가용 상태 (false=품절 처리, true=판매 재개)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.storeId").description("매장 식별자"),
                            fieldWithPath("data.ingredientId").description("원자재 마스터 식별자"),
                            fieldWithPath("data.name").description("원자재 명칭"),
                            fieldWithPath("data.stockQuantity").description("현재 매장 잔여 재고 수량"),
                            fieldWithPath("data.isAvailable").description("변경된 가용 상태"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("매장 특정 원자재 재고 수량 변경 API")
    fun updateStockQuantity() {
        val storeId = 1L
        val ingredientId = 200L
        val request = UpdateIngredientStockRequest(stockQuantity = 1)
        val updated = Ingredient(
            id = 2L,
            storeId = storeId,
            ingredientId = ingredientId,
            name = "우유",
            stockQuantity = 1,
            isAvailable = true,
        )
        given(partnerIngredientService.updateStockQuantity(storeId, ingredientId, 1)).willReturn(updated)

        mockMvc.perform(
            patch("/v1/partner/stores/{storeId}/ingredients/{ingredientId}/stock", storeId, ingredientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.stockQuantity").value(1))
            .andDo(
                document(
                    identifier = "partner-ingredient-update-stock",
                    tag = "Partner Ingredient",
                    summary = "매장 특정 원자재 재고 수량 변경 API",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("storeId").description("매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                            parameterWithName("ingredientId").description("원자재 마스터 식별자 (예: 200=우유)"),
                        ),
                        requestFields(
                            fieldWithPath("stockQuantity").description("변경할 재고 수량 (0 이상 정수)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.storeId").description("매장 식별자"),
                            fieldWithPath("data.ingredientId").description("원자재 마스터 식별자"),
                            fieldWithPath("data.name").description("원자재 명칭"),
                            fieldWithPath("data.stockQuantity").description("변경된 잔여 재고 수량"),
                            fieldWithPath("data.isAvailable").description("현재 가용 상태"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }
}
