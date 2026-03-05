package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EEventStatus {
    UPCOMING("Sắp diễn ra"),
    ONGOING("Đang diễn ra"),
    COMPLETED("Hoàn thành");

    private final String value;
}
