package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EPaymentMethod {
    CASH("Tiền mặt"),
    BANK_TRANSFER_ONLINE("Chuyển Khoản Online"),
    BANK_TRANSFER_OFFLINE("Chuyển Khoản");

    private final String value;
}
