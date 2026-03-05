package com.chiaseyeuthuong.dto.request;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.validator.EnumValue;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class EventRequest {

    @Min(value = 1, message = "Lỗi sự kiện")
    private Long id;

    @NotBlank(message = "Tên sự kiện không được để trống")
    private String name;

    @NotNull(message = "Thời gian bắt đầu sự kiện không được để trống")
    @FutureOrPresent(message = "Thời gian bắt đầu sự kiện phải trong hiện tại hoặc tương lai")
    private LocalDate startDate;

    @NotNull(message = "Thời gian kết thúc sự kiện không được để trống")
    @Future(message = "Thời gian kết thúc sự kiện phải trong tương lai")
    private LocalDate endDate;

    private BigDecimal currentAmount;

    private BigDecimal targetAmount;

    private String shortDescription;

    private String description;

    private String content;

    private String thumbnailUrl;

    @EnumValue(name = "status", enumClass = EEventStatus.class)
    @NotNull(message = "Vui lòng chọn trạng thái")
    private EEventStatus status;

    @Min(value = 1, message = "Danh mục không hợp lệ")
    @NotNull(message = "Danh mục chưa có")
    private Integer categoryId;
}
