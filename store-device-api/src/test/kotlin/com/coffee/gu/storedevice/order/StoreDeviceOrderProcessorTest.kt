package com.coffee.gu.storedevice.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.StoreDeviceType
import com.coffee.gu.order.ExternalOrderCreator
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class StoreDeviceOrderProcessorTest {

    private val externalOrderCreator: ExternalOrderCreator = mock()
    private val processor = StoreDeviceOrderProcessor(externalOrderCreator)

    private val kioskDevice = StoreDevicePrincipal(
        deviceId = 1L,
        deviceCode = "KIOSK-GANGNAM-01",
        storeId = 1L,
        deviceType = StoreDeviceType.KIOSK,
    )

    @Test
    @DisplayName("인증된 키오스크 기기 정보(storeId, deviceCode)와 KIOSK 채널로 주문을 생성한다")
    fun createKioskOrderSuccess() {
        val lines = listOf(NewOrderLine(menuId = 1L, quantity = 2L))
        val captor = argumentCaptor<NewOrder>()
        given(externalOrderCreator.create(captor.capture())).willReturn("ORDER-KEY-01")

        val orderKey = processor.create(kioskDevice, lines)

        assertThat(orderKey).isEqualTo("ORDER-KEY-01")
        verify(externalOrderCreator).create(captor.firstValue)
        assertThat(captor.firstValue.storeId).isEqualTo(1L)
        assertThat(captor.firstValue.channel).isEqualTo(OrderChannel.KIOSK)
        assertThat(captor.firstValue.principal).isEqualTo(Principal.guest("KIOSK-GANGNAM-01"))
        assertThat(captor.firstValue.lines).isEqualTo(lines)
    }

    @Test
    @DisplayName("인증된 POS 기기 정보(storeId, deviceCode)와 POS 채널로 주문을 생성한다")
    fun createPosOrderSuccess() {
        val posDevice = StoreDevicePrincipal(
            deviceId = 3L,
            deviceCode = "POS-GANGNAM-01",
            storeId = 1L,
            deviceType = StoreDeviceType.POS,
        )
        val lines = listOf(NewOrderLine(menuId = 2L, quantity = 1L))
        val captor = argumentCaptor<NewOrder>()
        given(externalOrderCreator.create(captor.capture())).willReturn("ORDER-KEY-POS-01")

        val orderKey = processor.create(posDevice, lines)

        assertThat(orderKey).isEqualTo("ORDER-KEY-POS-01")
        assertThat(captor.firstValue.storeId).isEqualTo(1L)
        assertThat(captor.firstValue.channel).isEqualTo(OrderChannel.POS)
        assertThat(captor.firstValue.principal).isEqualTo(Principal.guest("POS-GANGNAM-01"))
        assertThat(captor.firstValue.lines).isEqualTo(lines)
    }

    @Test
    @DisplayName("주문 항목이 비어 있거나 수량이 0 이하이면 INVALID_REQUEST 예외가 발생한다")
    fun failWhenLinesInvalid() {
        assertThatThrownBy { processor.create(kioskDevice, emptyList()) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_REQUEST)

        assertThatThrownBy { processor.create(kioskDevice, listOf(NewOrderLine(menuId = 1L, quantity = 0L))) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_REQUEST)
    }
}
