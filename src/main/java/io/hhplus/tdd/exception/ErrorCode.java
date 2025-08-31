package io.hhplus.tdd.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INVALID_BALANCE(new ErrorResponse("ERR-100", "잔액이 부족합니다."));

    private final ErrorResponse errorResponse;

    ErrorCode(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
