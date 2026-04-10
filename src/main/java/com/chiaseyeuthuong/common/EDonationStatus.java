package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EDonationStatus {
    PENDING_PAYMENT("Chờ thanh toán"),
    PENDING_APPROVED("Chờ duyệt"),
    CONFIRMED("Xác nhận"),
    CANCELLED("Huỷ"),
    REJECTED("Từ chối"),
    FAILED("Thất bại");

    private final String value;
}
