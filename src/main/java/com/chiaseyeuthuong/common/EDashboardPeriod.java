package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EDashboardPeriod {
    WEEK("1 tuần"),
    MONTH("1 tháng"),
    QUARTER("3 tháng"),
    YEAR("1 năm");

    private final String value;
}
