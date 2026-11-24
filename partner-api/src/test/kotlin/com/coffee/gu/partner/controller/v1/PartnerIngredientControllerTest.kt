package com.coffee.gu.partner.controller.v1

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.partner.auth.PartnerPrincipal
import com.coffee.gu.partner.auth.PartnerPrincipalArgumentResolver
import com.coffee.gu.partner.controller.PartnerRestDocsTest
import com.coffee.gu.partner.controller.v1.request.UpdateIngredientAvailabilityRequest
import com.coffee.gu.partner.menu.PartnerIngredientService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
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
class PartnerIngredientControllerTest : PartnerRestDocsTest() {

    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var partnerIngredientService: PartnerIngredientService

    @InjectMocks
    private lateinit var partnerIngredientController: PartnerIngredientController

    override val controller: Any
        get() = partnerIngredientController

    @Test
    @DisplayName("매장별 원자재 품절 상태 목록 조회 API")
    fun getStoreIngredients() {
        val storeId = 1L
        val partnerPrincipal = PartnerPrincipal(partnerId = "PARTNER-1", storeId = storeId)
        val ingredients = listOf(
            Ingredient(
                id = 1L,
                storeId = storeId,
                ingredientId = 100L,
                name = "에스프레소 원두",
                isAvailable = true,
            ),
            Ingredient(
                id = 2L,
                storeId = storeId,
                ingredientId = 200L,
                name = "우유",
                isAvailable = true,
            ),
        )
        given(partnerIngredientService.getStoreIngredients(partnerPrincipal, storeId)).willReturn(ingredients)

        mockMvc.perform(
            get("/v1/partner/stores/{storeId}/ingredients", storeId)
                .header(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER, "PARTNER-1")
                .header(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER, "1")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data[0].ingredientId").value(100))
            .andExpect(jsonPath("$.data[1].ingredientId").value(200))
            .andDo(
                document(
                    identifier = "partner-ingredient-list",
                    tag = "Partner Ingredient",
                    summary = "매장별 원자재 품절 상태 목록 조회 API",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER).description("점주(파트너) 식별자 (예: PARTNER-1)"),
                            headerWithName(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER).description("점주 소속 매장 식별자 (예: 1)"),
                        ),
                        pathParameters(
                            parameterWithName("storeId").description("매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data").description("매장 원자재 목록"),
                            fieldWithPath("data[].storeId").description("매장 식별자"),
                            fieldWithPath("data[].ingredientId").description("원자재 마스터 식별자 (100=원두, 200=우유, 300=바닐라시럽, 400=베이글)"),
                            fieldWithPath("data[].name").description("원자재 명칭"),
                            fieldWithPath("data[].isAvailable").description("판매 가용 여부 (false 시 수동 품절 상태)"),
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
        val partnerPrincipal = PartnerPrincipal(partnerId = "PARTNER-1", storeId = storeId)
        val request = UpdateIngredientAvailabilityRequest(isAvailable = false)
        val updated = Ingredient(
            id = 2L,
            storeId = storeId,
            ingredientId = ingredientId,
            name = "우유",
            isAvailable = false,
        )
        given(partnerIngredientService.updateAvailability(partnerPrincipal, storeId, ingredientId, false)).willReturn(updated)

        mockMvc.perform(
            patch("/v1/partner/stores/{storeId}/ingredients/{ingredientId}/availability", storeId, ingredientId)
                .header(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER, "PARTNER-1")
                .header(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER, "1")
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
                        requestHeaders(
                            headerWithName(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER).description("점주(파트너) 식별자 (예: PARTNER-1)"),
                            headerWithName(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER).description("점주 소속 매장 식별자 (예: 1)"),
                        ),
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
                            fieldWithPath("data.isAvailable").description("변경된 가용 상태"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("파트너 인증 헤더 없이 요청 시 401 UNAUTHORIZED를 반환한다")
    fun unauthorizedWithoutPartnerHeaders() {
        mockMvc.perform(
            get("/v1/partner/stores/{storeId}/ingredients", 1L)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E401"))
    }

    @Test
    @DisplayName("점주가 본인 소속 매장이 아닌 타 매장(storeId=2)의 재고 변경을 요청하면 403 FORBIDDEN을 반환한다")
    fun forbiddenWhenUpdatingOtherStoreIngredient() {
        val gangnamOwner = PartnerPrincipal(partnerId = "PARTNER-1", storeId = 1L)
        val otherStoreId = 2L
        val ingredientId = 200L
        val request = UpdateIngredientAvailabilityRequest(isAvailable = false)

        given(partnerIngredientService.updateAvailability(gangnamOwner, otherStoreId, ingredientId, false))
            .willThrow(CoreException(ErrorType.FORBIDDEN, "해당 매장에 대한 관리 권한이 없습니다."))

        mockMvc.perform(
            patch("/v1/partner/stores/{storeId}/ingredients/{ingredientId}/availability", otherStoreId, ingredientId)
                .header(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER, "PARTNER-1")
                .header(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E403"))
    }
}
