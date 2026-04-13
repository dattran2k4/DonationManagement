package com.chiaseyeuthuong.excel;

import org.springframework.web.multipart.MultipartFile;

public record ExcelImportRequest(
        MultipartFile file,
        String moduleLabel,
        String dataSheetName,
        HeaderValidator headerValidator,
        RowProcessor rowProcessor,
        ExcelImportMode mode,
        String errorReportFilename
) {
    public interface HeaderValidator {
        void validate(ExcelImportSheetContext context);
    }

    public interface RowProcessor {
        void process(ExcelImportSheetContext context, int rowIndex, int displayRowNumber);
    }
}
