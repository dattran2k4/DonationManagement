package com.chiaseyeuthuong.excel;

import com.chiaseyeuthuong.dto.response.ExcelImportErrorDetail;
import com.chiaseyeuthuong.dto.response.ExcelImportResult;
import com.chiaseyeuthuong.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EXCEL-IMPORT-EXECUTOR")
public class ExcelImportExecutionService {

    private static final int MAX_ERROR_LINES_IN_MESSAGE = 10;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.forLanguageTag("vi-VN"));

    private final PlatformTransactionManager transactionManager;
    private final ExcelErrorReportStorageService errorReportStorageService;
    private final ExcelErrorReportWorkbookBuilder errorReportWorkbookBuilder;

    public ExcelImportResult execute(ExcelImportRequest request) {
        try (Workbook workbook = openWorkbook(request.file())) {
            Sheet sheet = resolveSheet(workbook, request.dataSheetName());
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new InvalidDataException("Sai mẫu file import: không tìm thấy dòng tiêu đề");
            }

            Map<String, Integer> headers = buildHeaderMap(headerRow);
            if (headers.isEmpty()) {
                throw new InvalidDataException("Sai mẫu file import: không đọc được cột tiêu đề");
            }

            ExcelImportSheetContext context = new ExcelImportSheetContext(workbook, sheet, headers);
            request.headerValidator().validate(context);

            int totalRows = 0;
            int successCount = 0;
            List<ExcelImportErrorDetail> errorDetails = new ArrayList<>();
            ExcelImportMode mode = request.mode() != null ? request.mode() : ExcelImportMode.CONTINUE_ON_ERROR;

            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRowNumber = rowIndex + 1;
                final int currentRowIndex = rowIndex;
                final int currentDisplayRowNumber = displayRowNumber;

                try {
                    executeRowInNewTransaction(() -> request.rowProcessor().process(context, currentRowIndex, currentDisplayRowNumber));
                    successCount++;
                } catch (ExcelImportValidationException validationException) {
                    errorDetails.add(toImportErrorDetail(validationException));
                    log.warn("Import {} lỗi tại dòng {} cột {}: {}", request.moduleLabel(), displayRowNumber,
                            validationException.getColumnName(), validationException.getMessage());
                    if (mode == ExcelImportMode.FAIL_FAST) {
                        break;
                    }
                } catch (Exception exception) {
                    String message = extractMessage(exception);
                    errorDetails.add(ExcelImportErrorDetail.builder()
                            .rowNumber(displayRowNumber)
                            .columnName("Dữ liệu dòng")
                            .fieldKey(null)
                            .invalidValue(null)
                            .message(message)
                            .suggestion("Kiểm tra lại toàn bộ dữ liệu ở dòng này và thử import lại")
                            .build());
                    log.warn("Import {} lỗi tại dòng {}: {}", request.moduleLabel(), displayRowNumber, message, exception);
                    if (mode == ExcelImportMode.FAIL_FAST) {
                        break;
                    }
                }
            }

            if (totalRows == 0) {
                throw new InvalidDataException("File Excel không có dòng dữ liệu để nhập");
            }

            List<String> errors = errorDetails.stream()
                    .map(this::formatImportErrorDetail)
                    .toList();

            int failureCount = errorDetails.size();
            String errorReportToken = null;
            String errorReportFilename = null;

            if (!errorDetails.isEmpty()) {
                errorReportFilename = StringUtils.hasText(request.errorReportFilename())
                        ? request.errorReportFilename()
                        : "ket-qua-import-loi.xlsx";
                byte[] reportBytes = errorReportWorkbookBuilder.build(errorDetails);
                errorReportToken = errorReportStorageService.store(
                        reportBytes,
                        errorReportFilename,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                );
            }

            return ExcelImportResult.builder()
                    .module(request.moduleLabel())
                    .totalRows(totalRows)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .success(failureCount == 0)
                    .partialSuccess(successCount > 0 && failureCount > 0)
                    .errors(errors)
                    .errorDetails(errorDetails)
                    .errorReportToken(errorReportToken)
                    .errorReportFilename(errorReportFilename)
                    .message(buildImportMessage(request.moduleLabel(), totalRows, successCount, failureCount, errors))
                    .build();
        } catch (IOException exception) {
            throw new InvalidDataException("Không thể đọc file Excel. Vui lòng kiểm tra lại định dạng tệp");
        }
    }

    private void executeRowInNewTransaction(Runnable action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private Workbook openWorkbook(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidDataException("Vui lòng chọn file Excel để nhập");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
                throw new InvalidDataException("Chỉ hỗ trợ file Excel định dạng .xlsx hoặc .xls");
            }
        }

        return WorkbookFactory.create(file.getInputStream());
    }

    private Sheet resolveSheet(Workbook workbook, String expectedSheetName) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new InvalidDataException("File Excel không có sheet dữ liệu");
        }
        if (!StringUtils.hasText(expectedSheetName)) {
            return workbook.getSheetAt(0);
        }
        Sheet sheet = workbook.getSheet(expectedSheetName);
        if (sheet == null) {
            throw new InvalidDataException("Sai mẫu file import: không tìm thấy sheet " + expectedSheetName);
        }
        return sheet;
    }

    public Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        short lastCellNum = headerRow.getLastCellNum();
        for (int columnIndex = 0; columnIndex < lastCellNum; columnIndex++) {
            Cell cell = headerRow.getCell(columnIndex);
            String rawHeader = getCellString(cell);
            if (!StringUtils.hasText(rawHeader)) {
                continue;
            }
            headers.put(normalizeHeader(rawHeader), columnIndex);
        }
        return headers;
    }

    public boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        short firstCellNum = row.getFirstCellNum();
        short lastCellNum = row.getLastCellNum();
        if (firstCellNum < 0 || lastCellNum < 0) {
            return true;
        }

        for (int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue;
            }
            if (StringUtils.hasText(getCellString(cell))) {
                return false;
            }
        }
        return true;
    }

    public String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    public String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private ExcelImportErrorDetail toImportErrorDetail(ExcelImportValidationException exception) {
        return ExcelImportErrorDetail.builder()
                .rowNumber(exception.getRowNumber())
                .columnName(exception.getColumnName())
                .fieldKey(exception.getFieldKey())
                .invalidValue(exception.getInvalidValue())
                .message(exception.getMessage())
                .suggestion(exception.getSuggestion())
                .build();
    }

    private String formatImportErrorDetail(ExcelImportErrorDetail detail) {
        String columnPart = StringUtils.hasText(detail.getColumnName()) ? " - cột " + detail.getColumnName() : "";
        return "Dòng " + detail.getRowNumber() + columnPart + ": " + detail.getMessage();
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        String lastMessage = null;

        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                lastMessage = current.getMessage();
            }
            current = current.getCause();
        }

        return StringUtils.hasText(lastMessage) ? lastMessage : "Lỗi không xác định";
    }

    private String buildImportMessage(String moduleLabel,
                                      int totalRows,
                                      int successCount,
                                      int failureCount,
                                      List<String> errors) {
        String prefix;
        if (failureCount == 0) {
            prefix = String.format("Nhập Excel %s thành công: %d/%d dòng hợp lệ.", moduleLabel, successCount, totalRows);
        } else if (successCount == 0) {
            prefix = String.format("Nhập Excel %s thất bại: 0/%d dòng hợp lệ.", moduleLabel, totalRows);
        } else {
            prefix = String.format("Nhập Excel %s hoàn tất: %d/%d dòng thành công, %d dòng lỗi.", moduleLabel, successCount, totalRows, failureCount);
        }

        if (errors.isEmpty()) {
            return prefix;
        }

        List<String> summarizedErrors = errors.stream()
                .limit(MAX_ERROR_LINES_IN_MESSAGE)
                .toList();

        StringBuilder builder = new StringBuilder(prefix).append("\nLý do lỗi:");
        summarizedErrors.forEach(error -> builder.append("\n- ").append(error));

        if (errors.size() > MAX_ERROR_LINES_IN_MESSAGE) {
            builder.append("\n- ... và ").append(errors.size() - MAX_ERROR_LINES_IN_MESSAGE).append(" lỗi khác");
        }

        return builder.toString();
    }
}
