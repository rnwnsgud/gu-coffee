package com.coffee.gu.api.controller.v1;

import com.coffee.gu.api.controller.RestDocsTest;
import com.coffee.gu.api.controller.v1.request.CreatePaymentRequest;
import com.coffee.gu.coupon.IssuedCouponService;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderLine;
import com.coffee.gu.order.OrderService;
import com.coffee.gu.payment.PaymentApprovalResult;
import com.coffee.gu.payment.PaymentService;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentControllerTest extends RestDocsTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderService orderService;

    @Mock
    private IssuedCouponService issuedCouponService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object getController() {
        return paymentController;
    }

    @Test
    @DisplayName("결제 생성 API")
    void createPayment() throws Exception {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest("order-key", null);
        OrderLine orderLine = new OrderLine(1L, "order-key", 1L, "Coffee", "img", "desc", 1L, BigDecimal.valueOf(2000), BigDecimal.valueOf(2000), true);
        Order order = new Order("order-key", "Coffee", Principal.user("1"), 1L, BigDecimal.valueOf(2000), OrderState.CREATED, List.of(orderLine));

        given(orderService.getOrder(eq("order-key"), eq(OrderState.CREATED))).willReturn(order);
        given(issuedCouponService.getIssuedCouponsForCheckout(any(), any())).willReturn(List.of());
        given(paymentService.createPayment(any(), any())).willReturn(10L);

        // when & then
        mockMvc.perform(post("/v1/payments")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("payment-create",
                        requestHeaders(
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입")
                        ),
                        requestFields(
                                fieldWithPath("orderKey").description("주문 키"),
                                fieldWithPath("usedIssuedCouponId").description("사용할 쿠폰 식별자 (없으면 null)").optional()
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("data.paymentId").description("생성된 결제 식별자"),
                                fieldWithPath("error").description("에러 정보")
                        )
                ));
    }

    @Test
    @DisplayName("결제 승인 API")
    void confirm() throws Exception {
        // given
        Order order = new Order("order-key", "Coffee", Principal.user("1"), 1L, BigDecimal.valueOf(2000), OrderState.PAID, List.of());
        given(orderService.getOrder(eq(order.getKey()))).willReturn(order);
        given(paymentService.approvePayment(any())).willReturn(PaymentApprovalResult.approved("order-key", "payment-key", OffsetDateTime.now()));

        // when & then
        mockMvc.perform(post("/v1/payments/confirm")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                        .queryParam("orderId", "order-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("payment-confirm",
                        requestHeaders(
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입")
                        ),
                        queryParameters(
                                parameterWithName("orderId").description("주문 키")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("data.result").description("결제 상태"),
                                fieldWithPath("error").description("에러 정보")
                        )
                ));
    }

    @Test
    @DisplayName("결제 실패 콜백 API")
    void fail() throws Exception {
        // given
        Order order = new Order("order-key", "Coffee", Principal.user("1"), 1L, BigDecimal.valueOf(2000), OrderState.PAID, List.of());
        given(orderService.getOrder(eq(order.getKey()))).willReturn(order);

        // when & then
        mockMvc.perform(post("/v1/payments/fail")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                        .queryParam("orderId", "order-key")
                        .queryParam("code", "PAY_PROCESS_CANCELED")
                        .queryParam("message", "사용자가 결제를 취소했습니다"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("payment-fail",
                        requestHeaders(
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입")
                        ),
                        queryParameters(
                                parameterWithName("orderId").description("주문 키"),
                                parameterWithName("code").description("실패 코드"),
                                parameterWithName("message").description("실패 메시지")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("data").description("응답 데이터 (null)"),
                                fieldWithPath("error").description("에러 정보")
                        )
                ));
    }
}
