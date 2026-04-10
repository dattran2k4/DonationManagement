package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EUserStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Tạm khóa");

    private final String value;
}
