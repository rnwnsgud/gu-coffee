package com.coffee.gu.storedevice.pos

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.StoreDeviceType
import com.coffee.gu.order.ExternalOrderCanceler
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderRepository
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.math.BigDecimal

class PosOrderManagerTest {

    private val orderRepository: OrderRepository = mock()
    private val externalOrderCanceler: ExternalOrderCanceler = mock()
    private val manager = PosOrderManager(orderRepository, externalOrderCanceler)

    private val posDevice = StoreDevicePrincipal(
        deviceId = 3L,
        deviceCode = "POS-GANGNAM-01",
        storeId = 1L,
        deviceType = StoreDeviceType.POS,
    )

    private val kioskDevice = StoreDevicePrincipal(
        deviceId = 1L,
        deviceCode = "KIOSK-GANGNAM-01",
        storeId = 1L,
        deviceType = StoreDeviceType.KIOSK,
    )

    @Test
    @DisplayName("결제 전(CREATED) 매장 주문은 PG 환불 없이 즉시 CANCELED로 변경된다")
    fun cancelCreatedOrderDirectly() {
        val order = sampleOrder(key = "ORDER-CREATED-1", storeId = 1L, state = OrderState.CREATED)
        given(orderRepository.findByOrderKey("ORDER-CREATED-1")).willReturn(order)
        given(orderRepository.save(order)).willReturn(order)

        val result = manager.cancelStoreOrder(posDevice, "ORDER-CREATED-1")

        assertThat(result.state).isEqualTo(OrderState.CANCELED)
        verify(orderRepository).save(order)
        verify(externalOrderCanceler, never()).cancel(order)
    }

    @Test
    @DisplayName("결제 완료(PAID) 매장 주문은 ExternalOrderCanceler를 통해 환불 및 롤백 취소를 수행한다")
    fun cancelPaidOrderViaExternalCanceler() {
        val paidOrder = sampleOrder(key = "ORDER-PAID-1", storeId = 1L, state = OrderState.PAID)
        val canceledOrder = sampleOrder(key = "ORDER-PAID-1", storeId = 1L, state = OrderState.CANCELED)
        given(orderRepository.findByOrderKey("ORDER-PAID-1")).willReturn(paidOrder, canceledOrder)

        val result = manager.cancelStoreOrder(posDevice, "ORDER-PAID-1")

        assertThat(result.state).isEqualTo(OrderState.CANCELED)
        verify(externalOrderCanceler).cancel(paidOrder)
    }

    @Test
    @DisplayName("타 매장의 주문을 POS에서 취소 시도하면 FORBIDDEN으로 차단된다")
    fun rejectOtherStoreOrderCancel() {
        val otherStoreOrder = sampleOrder(key = "ORDER-OTHER-1", storeId = 2L, state = OrderState.CREATED)
        given(orderRepository.findByOrderKey("ORDER-OTHER-1")).willReturn(otherStoreOrder)

        assertThatThrownBy { manager.cancelStoreOrder(posDevice, "ORDER-OTHER-1") }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
    }

    @Test
    @DisplayName("KIOSK 기기로 POS 주문 취소 시도 시 FORBIDDEN으로 차단된다")
    fun rejectKioskDeviceCancel() {
        assertThatThrownBy { manager.cancelStoreOrder(kioskDevice, "ORDER-1") }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
    }

    @Test
    @DisplayName("이미 취소된 주문 재취소 시 INVALID_REQUEST 예외가 발생한다")
    fun rejectAlreadyCanceledOrder() {
        val canceledOrder = sampleOrder(key = "ORDER-CANCELED-1", storeId = 1L, state = OrderState.CANCELED)
        given(orderRepository.findByOrderKey("ORDER-CANCELED-1")).willReturn(canceledOrder)

        assertThatThrownBy { manager.cancelStoreOrder(posDevice, "ORDER-CANCELED-1") }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_REQUEST)
    }

    private fun sampleOrder(key: String, storeId: Long, state: OrderState): Order {
        return Order(
            key = key,
            name = "아메리카노",
            principal = Principal.guest("POS-GANGNAM-01"),
            storeId = storeId,
            totalPrice = BigDecimal("4500"),
            state = state,
            lines = emptyList(),
            channel = OrderChannel.POS,
        )
    }
}
