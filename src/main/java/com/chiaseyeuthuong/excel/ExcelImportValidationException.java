package com.chiaseyeuthuong.excel;

import lombok.Getter;

@Getter
public class ExcelImportValidationException extends RuntimeException {
    private final Integer rowNumber;
    private final String columnName;
    private final String fieldKey;
    private final String invalidValue;
    private final String suggestion;

    public ExcelImportValidationException(Integer rowNumber,
                                          String columnName,
                                          String fieldKey,
                                          String invalidValue,
                                          String message,
                                          String suggestion) {
        super(message);
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.fieldKey = fieldKey;
        this.invalidValue = invalidValue;
        this.suggestion = suggestion;
    }
}
