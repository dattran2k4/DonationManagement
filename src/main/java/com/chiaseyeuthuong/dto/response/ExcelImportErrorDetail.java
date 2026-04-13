package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelImportErrorDetail {
    private Integer rowNumber;
    private String columnName;
    private String fieldKey;
    private String invalidValue;
    private String message;
    private String suggestion;
}
