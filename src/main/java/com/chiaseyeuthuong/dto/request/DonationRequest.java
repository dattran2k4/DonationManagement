package com.chiaseyeuthuong.dto.request;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.validator.EnumValue;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class DonationRequest {

    @NotNull(message = "Số tiền không được để trống")
    @Positive
    @DecimalMin(value = "1000", message = "Số tiền tối thiểu là 1.000 đồng")
    private BigDecimal amount;

    private String message;

    private Boolean needReceipt;

    private String receiptName;

    private String receiptEmail;

    @EnumValue(name = "paymentMethod", enumClass = EPaymentMethod.class)
    @NotNull(message = "Chọn phương thức thanh toán")
    private EPaymentMethod paymentMethod;

    @Min(1)
    private Long eventId;

    @Min(1)
    private Long activityId;

    @NotNull(message = "Người tài trợ không được để trống")
    @Min(1)
    private Long donorId;
}
