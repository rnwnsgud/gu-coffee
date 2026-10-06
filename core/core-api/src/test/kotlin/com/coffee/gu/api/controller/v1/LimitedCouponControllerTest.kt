package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.coupon.Coupon
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.coupon.LimitedCoupon
import com.coffee.gu.coupon.LimitedCouponService
import com.coffee.gu.enums.CouponType
import com.coffee.gu.enums.IssuedCouponState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

@MockitoSettings(strictness = Strictness.LENIENT)
class LimitedCouponControllerTest : RestDocsTest() {

    @Mock
    private lateinit var limitedCouponService: LimitedCouponService

    @InjectMocks
    private lateinit var limitedCouponController: LimitedCouponController

    override val controller: Any
        get() = limitedCouponController

    @Test
    @DisplayName("선착순 쿠폰 상세 및 잔여 재고 조회 API")
    fun getLimitedCoupon() {
        val coupon = Coupon(
            id = 1L,
            name = "선착순 3000원 할인 쿠폰",
            type = CouponType.FIXED_AMOUNT,
            discount = BigDecimal.valueOf(3000),
            expiredAt = LocalDateTime.of(2026, 12, 31, 23, 59, 59),
        )
        val limitedCoupon = LimitedCoupon(
            id = 1L,
            coupon = coupon,
            totalQuantity = 100,
            issuedQuantity = 30,
        )

        given(limitedCouponService.get(1L)).willReturn(limitedCoupon)

        mockMvc.perform(
            get("/v1/limited-coupons/{limitedCouponId}", 1L)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("선착순 3000원 할인 쿠폰"))
            .andExpect(jsonPath("$.data.totalQuantity").value(100))
            .andExpect(jsonPath("$.data.issuedQuantity").value(30))
            .andExpect(jsonPath("$.data.remainingQuantity").value(70))
            .andDo(
                document(
                    "limited-coupon-get",
                    pathParameters(
                        parameterWithName("limitedCouponId").description("선착순 쿠폰 식별자")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.id").description("선착순 쿠폰 식별자"),
                        fieldWithPath("data.name").description("쿠폰 명칭"),
                        fieldWithPath("data.type").description("쿠폰 할인 타입 (FIXED_AMOUNT / PERCENTAGE)"),
                        fieldWithPath("data.discount").description("할인 금액 또는 할인율"),
                        fieldWithPath("data.expiredAt").description("쿠폰 만료 일시"),
                        fieldWithPath("data.totalQuantity").description("총 한정 수량"),
                        fieldWithPath("data.issuedQuantity").description("현재 발급된 수량"),
                        fieldWithPath("data.remainingQuantity").description("잔여 수량"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("선착순 쿠폰 발급/응모 API")
    fun issueLimitedCoupon() {
        val coupon = Coupon(
            id = 1L,
            name = "선착순 3000원 할인 쿠폰",
            type = CouponType.FIXED_AMOUNT,
            discount = BigDecimal.valueOf(3000),
            expiredAt = LocalDateTime.of(2026, 12, 31, 23, 59, 59),
        )
        val issuedCoupon = IssuedCoupon(
            id = 10L,
            principal = com.coffee.gu.Principal("1", com.coffee.gu.enums.PrincipalType.USER),
            state = IssuedCouponState.DOWNLOADED,
            coupon = coupon,
        )

        given(limitedCouponService.issue(any(), any())).willReturn(issuedCoupon)

        mockMvc.perform(
            post("/v1/limited-coupons/{limitedCouponId}/issue", 1L)
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(10L))
            .andExpect(jsonPath("$.data.state").value("DOWNLOADED"))
            .andExpect(jsonPath("$.data.name").value("선착순 3000원 할인 쿠폰"))
            .andDo(
                document(
                    "limited-coupon-issue",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    pathParameters(
                        parameterWithName("limitedCouponId").description("선착순 쿠폰 식별자")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.id").description("발급된 쿠폰 식별자"),
                        fieldWithPath("data.state").description("발급된 쿠폰 상태 (DOWNLOADED, USED, CANCELED)"),
                        fieldWithPath("data.name").description("쿠폰 명칭"),
                        fieldWithPath("data.type").description("쿠폰 할인 타입 (FIXED_AMOUNT / PERCENTAGE)"),
                        fieldWithPath("data.discount").description("할인 금액 또는 할인율"),
                        fieldWithPath("data.expiredAt").description("쿠폰 만료 일시"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }
}
