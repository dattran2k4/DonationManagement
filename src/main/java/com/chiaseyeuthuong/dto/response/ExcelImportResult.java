package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcelImportResult {
    private String module;
    private int totalRows;
    private int successCount;
    private int failureCount;
    private boolean success;
    private boolean partialSuccess;
    private List<String> errors;
    private List<ExcelImportErrorDetail> errorDetails;
    private String errorReportToken;
    private String errorReportFilename;
    private String message;
}
