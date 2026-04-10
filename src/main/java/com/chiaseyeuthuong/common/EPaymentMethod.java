package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EPaymentMethod {
    CASH("Tiền mặt"),
    BANK_TRANSFER_ONLINE("Chuyển khoản qua website"),
    BANK_TRANSFER_OFFLINE("Chuyển khoản không qua website");

    private final String value;
}
