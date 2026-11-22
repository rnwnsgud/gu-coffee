package com.coffee.gu.storedevice.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.device.StoreDevice
import com.coffee.gu.device.StoreDeviceRepository
import com.coffee.gu.enums.StoreDeviceType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock

class StoreDeviceAuthenticatorTest {

    private val storeDeviceRepository: StoreDeviceRepository = mock()
    private val authenticator = StoreDeviceAuthenticator(storeDeviceRepository)

    @Test
    @DisplayName("유효한 활성 기기 API Key로 인증 시 StoreDevicePrincipal을 반환한다")
    fun authenticateSuccess() {
        val rawKey = "kiosk-gangnam-01-secret-key"
        val hash = DeviceApiKeyHasher.sha256Hex(rawKey)
        given(storeDeviceRepository.findByApiKeyHash(hash)).willReturn(
            StoreDevice(
                id = 1L,
                deviceCode = "KIOSK-GANGNAM-01",
                storeId = 1L,
                deviceType = StoreDeviceType.KIOSK,
                apiKeyHash = hash,
                isActive = true,
            ),
        )

        val principal = authenticator.authenticate(rawKey)

        assertThat(principal.deviceId).isEqualTo(1L)
        assertThat(principal.deviceCode).isEqualTo("KIOSK-GANGNAM-01")
        assertThat(principal.storeId).isEqualTo(1L)
        assertThat(principal.deviceType).isEqualTo(StoreDeviceType.KIOSK)
    }

    @Test
    @DisplayName("API Key 헤더가 누락되거나 공백이면 UNAUTHORIZED 예외가 발생한다")
    fun failWhenMissingApiKey() {
        assertThatThrownBy { authenticator.authenticate(null) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)

        assertThatThrownBy { authenticator.authenticate("   ") }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }

    @Test
    @DisplayName("등록되지 않은 API Key이면 UNAUTHORIZED 예외가 발생한다")
    fun failWhenUnknownApiKey() {
        val rawKey = "unknown-device-key"
        val hash = DeviceApiKeyHasher.sha256Hex(rawKey)
        given(storeDeviceRepository.findByApiKeyHash(hash)).willReturn(null)

        assertThatThrownBy { authenticator.authenticate(rawKey) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }

    @Test
    @DisplayName("비활성화된 기기의 API Key이면 FORBIDDEN 예외가 발생한다")
    fun failWhenInactiveDevice() {
        val rawKey = "inactive-kiosk-key"
        val hash = DeviceApiKeyHasher.sha256Hex(rawKey)
        given(storeDeviceRepository.findByApiKeyHash(hash)).willReturn(
            StoreDevice(
                id = 2L,
                deviceCode = "KIOSK-GANGNAM-02",
                storeId = 1L,
                deviceType = StoreDeviceType.KIOSK,
                apiKeyHash = hash,
                isActive = false,
            ),
        )

        assertThatThrownBy { authenticator.authenticate(rawKey) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
    }
}
