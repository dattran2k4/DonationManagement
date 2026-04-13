package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDetailTabsSummaryResponse {
    private long activityCount;
    private long donorCount;
    private long donationCount;
    private long auditLogCount;
}
