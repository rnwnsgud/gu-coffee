package com.coffee.gu;

public enum ErrorType {

    DEFAULT_ERROR(500, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", ErrorLogLevel.ERROR),
    SYSTEM_LOGIC_ERROR(500, ErrorCode.E500, "시스템 내부 로직 오류가 발생했습니다.", ErrorLogLevel.ERROR),
    INVALID_REQUEST(400, ErrorCode.E400, "요청이 올바르지 않습니다.", ErrorLogLevel.INFO),
    UNAUTHORIZED(401, ErrorCode.E401, "인증된 사용자가 아닙니다.", ErrorLogLevel.INFO),
    NOT_FOUND_DATA(404, ErrorCode.E404, "해당 데이터를 찾을 수 없습니다.", ErrorLogLevel.ERROR),

    // 주문
    ORDER_ALREADY_PAID(400, ErrorCode.E1000, "이미 결제가 완료된 주문입니다.", ErrorLogLevel.INFO),

    // 결제
    PAYMENT_INVALID_STATE(400, ErrorCode.E2000, "결제 상태가 유효하지 않습니다.", ErrorLogLevel.INFO),
    PAYMENT_AMOUNT_MISMATCH(400, ErrorCode.E2001, "결제 금액이 일치하지 않습니다.", ErrorLogLevel.INFO),
    PAYMENT_INVALID_AMOUNT(400, ErrorCode.E2002, "결제 금액이 0보다 작을 수 없습니다.", ErrorLogLevel.INFO),
    PAYMENT_FAIL(502, ErrorCode.E2003, "결제 실패했습니다. 잠시 후 다시 시도해주세요.", ErrorLogLevel.WARN),


    // 메뉴
    MENU_MISMATCH_IN_ORDER(400, ErrorCode.E3000, "요청한 상품 정보와 일치하지 않습니다.", ErrorLogLevel.INFO),

    // 쿠폰
    COUPON_NOT_FOUND_OR_EXPIRED(400, ErrorCode.E4000, "쿠폰을 찾을 수 없거나 만료되었습니다.", ErrorLogLevel.INFO),
    COUPON_ALREADY_DOWNLOADED(400, ErrorCode.E4001, "이미 다운로드한 쿠폰입니다.", ErrorLogLevel.INFO),

    // 발행 쿠폰
    ISSUED_COUPON_INVALID(400, ErrorCode.E5000, "사용할 수 없는 쿠폰입니다.", ErrorLogLevel.INFO);



    private final Integer status;
    private final ErrorCode code;
    private final String message;
    private final ErrorLogLevel logLevel;

    ErrorType(Integer status, ErrorCode code, String message, ErrorLogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public ErrorCode getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }

    public Integer getStatus() {
        return status;
    }

    public ErrorLogLevel getErrorLogLevel() {
        return logLevel;
    }
}
