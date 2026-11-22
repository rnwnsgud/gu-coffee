package com.coffee.gu.storedevice.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.StoreDeviceType

data class StoreDevicePrincipal(
    val deviceId: Long,
    val deviceCode: String,
    val storeId: Long,
    val deviceType: StoreDeviceType,
) {
    init {
        if (deviceId <= 0 || deviceCode.isBlank() || storeId <= 0) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 매장 기기 인증 정보입니다.")
        }
    }

    fun validatePosDevice() {
        if (deviceType != StoreDeviceType.POS) {
            throw CoreException(ErrorType.FORBIDDEN, "POS 기기에서만 사용할 수 있는 기능입니다.")
        }
    }
}
