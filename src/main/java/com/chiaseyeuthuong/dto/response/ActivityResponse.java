package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EActivityStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ActivityResponse {
    private Long id;

    private Long eventId;

    private String name;

    private String slug;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal currentAmount;

    private BigDecimal targetAmount;

    private String shortDescription;

    private String description;

    private String location;

    private String content;

    private String thumbnailUrl;

    private long numberOfDonors;

    private EActivityStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private EventResponse event;
}
