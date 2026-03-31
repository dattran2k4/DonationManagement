package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityDetailTabsSummaryResponse {
    private long donorCount;
    private long donationCount;
}
