package com.coffee.gu.storedevice.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.device.StoreDeviceRepository
import org.springframework.stereotype.Component

@Component
class StoreDeviceAuthenticator(
    private val storeDeviceRepository: StoreDeviceRepository,
) {
    fun authenticate(rawApiKey: String?): StoreDevicePrincipal {
        val normalized = rawApiKey?.trim()
        if (normalized.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "매장 기기 API Key 헤더가 누락되었습니다.")
        }

        val apiKeyHash = DeviceApiKeyHasher.sha256Hex(normalized)
        val device = storeDeviceRepository.findByApiKeyHash(apiKeyHash)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 매장 기기 API Key입니다.")

        if (!DeviceApiKeyHasher.matches(device.apiKeyHash, apiKeyHash)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 매장 기기 API Key입니다.")
        }

        if (!device.isActive) {
            throw CoreException(ErrorType.FORBIDDEN, "비활성화된 매장 기기입니다.")
        }

        return StoreDevicePrincipal(
            deviceId = device.id,
            deviceCode = device.deviceCode,
            storeId = device.storeId,
            deviceType = device.deviceType,
        )
    }
}
