package com.chiaseyeuthuong.dto.request;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.dto.validator.EnumValue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ActivityRequest {

    @Min(value = 1)
    private Long id;

    @NotBlank(message = "Tên hoạt động không được để trống")
    private String name;

    private String shortDescription;

    private String content;

    private LocalDate startDate;

    private LocalDate endDate;

    @EnumValue(name = "status", enumClass = EActivityStatus.class)
    private EActivityStatus status;

    private BigDecimal currentAmount;

    private BigDecimal targetAmount;

    private String thumbnailUrl;

    @Min(value = 1)
    @NotNull(message = "Chưa có sự kiện cụ thể")
    private Long eventId;
}
