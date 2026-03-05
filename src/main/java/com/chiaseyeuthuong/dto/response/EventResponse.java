package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EEventStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EventResponse {

    private Long id;

    private Integer categoryId;

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

    private EEventStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private CategoryResponse category;

    private List<ActivityResponse> activities = new ArrayList<>();
}
