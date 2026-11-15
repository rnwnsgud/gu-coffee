package com.coffee.gu.partner.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType

data class PartnerPrincipal(
    val partnerId: String,
    val storeId: Long,
) {
    init {
        if (partnerId.isBlank() || storeId <= 0) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 파트너 인증 정보입니다.")
        }
    }

    fun validateStoreOwnership(targetStoreId: Long) {
        if (this.storeId != targetStoreId) {
            throw CoreException(ErrorType.FORBIDDEN, "해당 매장에 대한 관리 권한이 없습니다.")
        }
    }
}
