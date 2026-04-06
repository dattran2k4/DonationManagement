package com.chiaseyeuthuong.dto.request;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.validator.EnumValue;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class EventRequest {

    @NotBlank(message = "Tên sự kiện không được để trống")
    private String name;

    @NotNull(message = "Thời gian bắt đầu sự kiện không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Thời gian kết thúc sự kiện không được để trống")
    private LocalDate endDate;

    private BigDecimal currentAmount;

    private BigDecimal targetAmount;

    private String shortDescription;

    private String location;

    private String content;

    private String thumbnailUrl;

    @EnumValue(name = "status", enumClass = EEventStatus.class)
    private EEventStatus status;

    @Min(value = 1, message = "Danh mục không hợp lệ")
    @NotNull(message = "Danh mục chưa có")
    private Integer categoryId;
}
