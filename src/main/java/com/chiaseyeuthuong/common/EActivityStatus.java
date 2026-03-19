package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EActivityStatus {
    DRAFT("Bản nháp"),
    UPCOMING("Sắp diễn ra"),
    ONGOING("Đang diễn ra"),
    COMPLETED("Hoàn thành");

    private final String value;
}
