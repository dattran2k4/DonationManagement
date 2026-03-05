package com.chiaseyeuthuong.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EDonationTarget {
    EVENT("Chiến dịch"),
    ACTIVITY("Hoạt động"),
    NONE("Thiện nguyện");

    private final String value;
}