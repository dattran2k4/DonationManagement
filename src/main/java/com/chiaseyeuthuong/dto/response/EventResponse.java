package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EEventStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
