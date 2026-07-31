package com.coffee.gu.menu;



import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;

import java.math.BigDecimal;

public record Price(
        BigDecimal costPrice,
        BigDecimal salesPrice
) {
    public Price {
        if (salesPrice.compareTo(costPrice) < 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "판매가는 정가보다 낮을 수 없습니다.");
        }
    }

}
