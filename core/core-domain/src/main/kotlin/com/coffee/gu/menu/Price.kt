package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import java.math.BigDecimal

class Price(
    val costPrice: BigDecimal,
    val salesPrice: BigDecimal,
) {
    init {
        if (salesPrice < costPrice) {
            throw CoreException(ErrorType.INVALID_REQUEST, "판매가는 정가보다 낮을 수 없습니다.")
        }
    }
}
