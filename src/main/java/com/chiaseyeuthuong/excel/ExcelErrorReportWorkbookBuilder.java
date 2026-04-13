package com.chiaseyeuthuong.excel;

import com.chiaseyeuthuong.dto.response.ExcelImportErrorDetail;
import com.chiaseyeuthuong.exception.InvalidDataException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ExcelErrorReportWorkbookBuilder {

    private final ExcelWorkbookSupport workbookSupport = new ExcelWorkbookSupport();

    public byte[] build(List<ExcelImportErrorDetail> errorDetails) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ExcelWorkbookSupport.WorkbookStyles styles = workbookSupport.createStyles(workbook);
            var sheet = workbook.createSheet("BaoCaoLoiImport");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Dòng", "Cột", "Mã trường", "Giá trị nhập", "Lý do lỗi", "Gợi ý sửa"};
            for (int i = 0; i < headers.length; i++) {
                workbookSupport.writeTextCell(headerRow, i, headers[i], styles.errorHeaderStyle());
            }

            int rowIndex = 1;
            for (ExcelImportErrorDetail errorDetail : errorDetails) {
                Row row = sheet.createRow(rowIndex++);
                workbookSupport.writeTextCell(row, 0, errorDetail.getRowNumber() != null ? String.valueOf(errorDetail.getRowNumber()) : "", styles.referenceCellStyle());
                workbookSupport.writeTextCell(row, 1, errorDetail.getColumnName(), styles.referenceCellStyle());
                workbookSupport.writeTextCell(row, 2, errorDetail.getFieldKey(), styles.referenceCellStyle());
                workbookSupport.writeTextCell(row, 3, errorDetail.getInvalidValue(), styles.errorCellStyle());
                workbookSupport.writeTextCell(row, 4, errorDetail.getMessage(), styles.referenceCellStyle());
                workbookSupport.writeTextCell(row, 5, errorDetail.getSuggestion(), styles.referenceCellStyle());
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
            workbookSupport.autosizeSheet(sheet, headers.length);
            return workbookSupport.toByteArray(workbook);
        } catch (IOException exception) {
            throw new InvalidDataException("Không thể tạo file báo cáo lỗi import");
        }
    }
}
