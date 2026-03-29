package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcelImportResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<String> errors;
    private String message;
}
