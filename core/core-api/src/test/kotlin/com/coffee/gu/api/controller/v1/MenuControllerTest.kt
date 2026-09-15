package com.coffee.gu.api.controller.v1

import com.coffee.gu.Page
import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.enums.MenuType
import com.coffee.gu.menu.Menu
import com.coffee.gu.menu.MenuDetail
import com.coffee.gu.menu.MenuDetailResult
import com.coffee.gu.menu.MenuService
import com.coffee.gu.menu.Nutrition
import com.coffee.gu.menu.Price
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.node.JsonNodeType
import java.math.BigDecimal
import java.time.LocalDateTime

@MockitoSettings(strictness = Strictness.LENIENT)
class MenuControllerTest : RestDocsTest() {

    @Mock
    private lateinit var menuService: MenuService

    @InjectMocks
    private lateinit var menuController: MenuController

    override val controller: Any
        get() = menuController

    @Test
    @DisplayName("메뉴 목록 조회 API (v1)")
    fun getMenusV1() {
        // given
        val menu = Menu(1L, "Americano", MenuType.DRINK, Price(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)), null, null, null)
        val page = Page(listOf(menu), false, null, null)
        given(menuService.findMenus(eq(1L), any())).willReturn(page)

        // when & then
        mockMvc.perform(
            get("/v1/menus")
                .param("categoryId", "1")
                .param("offset", "0")
                .param("limit", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "menu-list-v1",
                    queryParameters(
                        parameterWithName("categoryId").description("카테고리 식별자"),
                        parameterWithName("offset").description("오프셋"),
                        parameterWithName("limit").description("조회 제한 수")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.content").description("메뉴 목록"),
                        fieldWithPath("data.content[].id").description("메뉴 식별자"),
                        fieldWithPath("data.content[].name").description("메뉴 이름"),
                        fieldWithPath("data.content[].salesPrice").description("판매가"),
                        fieldWithPath("data.content[].imageUrl").description("이미지 URL").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.hasNext").description("다음 페이지 여부"),
                        fieldWithPath("data.nextCursor").description("다음 페이지 커서 (v1 미사용)").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.nextLastId").description("다음 페이지 마지막 ID (v1 미사용)").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("메뉴 목록 조회 API (v2 - Cursor)")
    fun getMenusV2() {
        // given
        val menu = Menu(1L, "Americano", MenuType.DRINK, Price(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)), null, null, null)
        val page = Page(listOf(menu), false, null, null)
        given(menuService.findMenus(eq(1L), any(), any(), any())).willReturn(page)

        // when & then
        mockMvc.perform(
            get("/v2/menus")
                .param("categoryId", "1")
                .param("pageSize", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "menu-list-v2",
                    queryParameters(
                        parameterWithName("categoryId").description("카테고리 식별자"),
                        parameterWithName("pageSize").description("페이지 크기").optional(),
                        parameterWithName("cursor").description("커서 (생성일시)").optional(),
                        parameterWithName("lastId").description("마지막 조회 ID").optional()
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.content").description("메뉴 목록"),
                        fieldWithPath("data.content[].id").description("메뉴 식별자"),
                        fieldWithPath("data.content[].name").description("메뉴 이름"),
                        fieldWithPath("data.content[].salesPrice").description("판매가"),
                        fieldWithPath("data.content[].imageUrl").description("이미지 URL").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.hasNext").description("다음 페이지 여부"),
                        fieldWithPath("data.nextCursor").description("다음 페이지 커서").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.nextLastId").description("다음 페이지 마지막 ID").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("메뉴 상세 조회 API")
    fun getMenu() {
        // given
        val menu = Menu(
            1L, "Americano", MenuType.DRINK, Price(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)),
            "http://image.com", "Cold Brew",
            MenuDetail(
                Nutrition(355.0, 150.0, 10.0, 5.0, 2.0, 1.0, 0.0, 0.0, 0.5),
                "Milk", "Soy"
            )
        )
        val result = MenuDetailResult(menu, emptyList(), emptyList())
        given(menuService.getMenu(1L)).willReturn(result)

        // when & then
        mockMvc.perform(get("/v1/menus/{menuId}", 1L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "menu-detail",
                    pathParameters(
                        parameterWithName("menuId").description("메뉴 식별자")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.id").description("메뉴 식별자"),
                        fieldWithPath("data.name").description("메뉴 이름"),
                        fieldWithPath("data.salesPrice").description("판매가"),
                        fieldWithPath("data.imageUrl").description("이미지 URL").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.description").description("메뉴 설명").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.capacity").description("용량").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.caffeine").description("카페인").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.calories").description("칼로리").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.sodium").description("나트륨").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.carbohydrate").description("탄수화물").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.sugar").description("당류").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.fat").description("지방").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.saturatedFat").description("포화지방").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.protein").description("단백질").optional().type(JsonNodeType.NUMBER),
                        fieldWithPath("data.containedAllergens").description("함유 알레르기 성분").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.mayContainAllergens").description("교차 오염 가능 성분").optional().type(JsonNodeType.STRING),
                        fieldWithPath("data.optionGroups").description("옵션 그룹 목록"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }
}
