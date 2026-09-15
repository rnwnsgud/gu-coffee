package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.coupon.CouponService
import com.coffee.gu.coupon.IssuedCouponService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@MockitoSettings(strictness = Strictness.LENIENT)
class CouponControllerTest : RestDocsTest() {

    @Mock
    private lateinit var couponService: CouponService

    @Mock
    private lateinit var issuedCouponService: IssuedCouponService

    @InjectMocks
    private lateinit var couponController: CouponController

    override val controller: Any
        get() = couponController

    @Test
    @DisplayName("발급된 쿠폰 목록 조회 API")
    fun getIssuedCoupons() {
        // given
        given(issuedCouponService.getIssuedCoupons(any())).willReturn(emptyList())

        // when & then
        mockMvc.perform(
            get("/v1/issued-coupons")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "coupon-issued-list",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data").description("발급된 쿠폰 목록"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("쿠폰 다운로드 API")
    fun download() {
        // when & then
        mockMvc.perform(
            post("/v1/coupons/{couponId}/download", 1L)
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "coupon-download",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    pathParameters(
                        parameterWithName("couponId").description("쿠폰 식별자")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data").description("응답 데이터 (null)"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }
}
