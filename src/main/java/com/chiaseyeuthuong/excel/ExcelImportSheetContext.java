package com.chiaseyeuthuong.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

public record ExcelImportSheetContext(
        Workbook workbook,
        Sheet sheet,
        Map<String, Integer> headers
) {
    public Row rowAt(int rowIndex) {
        return sheet.getRow(rowIndex);
    }
}
