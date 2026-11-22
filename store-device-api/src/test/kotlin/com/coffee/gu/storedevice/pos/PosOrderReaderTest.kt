package com.coffee.gu.storedevice.pos

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.StoreDeviceType
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderRepository
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.math.BigDecimal

class PosOrderReaderTest {

    private val orderRepository: OrderRepository = mock()
    private val reader = PosOrderReader(orderRepository)

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
    @DisplayName("POS 기기는 소속 매장의 주문 목록을 조회할 수 있다")
    fun findStoreOrdersSuccess() {
        val order = Order(
            key = "ORDER-1",
            name = "아메리카노",
            principal = Principal.guest("POS-GANGNAM-01"),
            storeId = 1L,
            totalPrice = BigDecimal("4500"),
            state = OrderState.PAID,
            lines = emptyList(),
            channel = OrderChannel.POS,
        )
        given(orderRepository.findAllByStoreId(1L, OrderState.PAID)).willReturn(listOf(order))

        val result = reader.findStoreOrders(posDevice, OrderState.PAID)

        assertThat(result).hasSize(1)
        assertThat(result.first().key).isEqualTo("ORDER-1")
    }

    @Test
    @DisplayName("KIOSK 기기로 POS 주문 목록 조회 시 FORBIDDEN으로 차단된다")
    fun rejectNonPosDevice() {
        assertThatThrownBy { reader.findStoreOrders(kioskDevice, null) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
    }
}
