package com.chiaseyeuthuong.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelWorkbookSupport {

    public WorkbookStyles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        Font whiteBoldFont = workbook.createFont();
        whiteBoldFont.setBold(true);
        whiteBoldFont.setColor(IndexedColors.WHITE.getIndex());
        whiteBoldFont.setFontHeightInPoints((short) 11);
        whiteBoldFont.setFontName("Arial");

        Font defaultFont = workbook.createFont();
        defaultFont.setFontName("Arial");
        defaultFont.setFontHeightInPoints((short) 10);

        Font boldFont = workbook.createFont();
        boldFont.setFontName("Arial");
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setBold(true);

        CellStyle requiredHeaderStyle = workbook.createCellStyle();
        requiredHeaderStyle.setFont(whiteBoldFont);
        requiredHeaderStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        requiredHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        requiredHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        requiredHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        requiredHeaderStyle.setWrapText(true);
        setAllBorders(requiredHeaderStyle, BorderStyle.THIN);

        CellStyle optionalHeaderStyle = workbook.createCellStyle();
        optionalHeaderStyle.cloneStyleFrom(requiredHeaderStyle);
        optionalHeaderStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());

        CellStyle guideHeaderStyle = workbook.createCellStyle();
        guideHeaderStyle.cloneStyleFrom(requiredHeaderStyle);
        guideHeaderStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());

        CellStyle oddRowTextStyle = workbook.createCellStyle();
        oddRowTextStyle.setFont(defaultFont);
        oddRowTextStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        oddRowTextStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        oddRowTextStyle.setVerticalAlignment(VerticalAlignment.TOP);
        oddRowTextStyle.setWrapText(true);
        setAllBorders(oddRowTextStyle, BorderStyle.THIN);

        CellStyle evenRowTextStyle = workbook.createCellStyle();
        evenRowTextStyle.cloneStyleFrom(oddRowTextStyle);
        evenRowTextStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());

        CellStyle oddRowAmountStyle = workbook.createCellStyle();
        oddRowAmountStyle.cloneStyleFrom(oddRowTextStyle);
        oddRowAmountStyle.setDataFormat(dataFormat.getFormat("#,##0"));

        CellStyle evenRowAmountStyle = workbook.createCellStyle();
        evenRowAmountStyle.cloneStyleFrom(evenRowTextStyle);
        evenRowAmountStyle.setDataFormat(dataFormat.getFormat("#,##0"));

        CellStyle oddRowDateStyle = workbook.createCellStyle();
        oddRowDateStyle.cloneStyleFrom(oddRowTextStyle);
        oddRowDateStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));

        CellStyle evenRowDateStyle = workbook.createCellStyle();
        evenRowDateStyle.cloneStyleFrom(evenRowTextStyle);
        evenRowDateStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));

        CellStyle oddRowDateTimeStyle = workbook.createCellStyle();
        oddRowDateTimeStyle.cloneStyleFrom(oddRowTextStyle);
        oddRowDateTimeStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm"));

        CellStyle evenRowDateTimeStyle = workbook.createCellStyle();
        evenRowDateTimeStyle.cloneStyleFrom(evenRowTextStyle);
        evenRowDateTimeStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm"));

        CellStyle guideCellStyle = workbook.createCellStyle();
        guideCellStyle.setFont(defaultFont);
        guideCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        guideCellStyle.setWrapText(true);
        setAllBorders(guideCellStyle, BorderStyle.THIN);

        CellStyle errorHeaderStyle = workbook.createCellStyle();
        errorHeaderStyle.cloneStyleFrom(requiredHeaderStyle);
        errorHeaderStyle.setFillForegroundColor(IndexedColors.RED.getIndex());

        CellStyle errorCellStyle = workbook.createCellStyle();
        errorCellStyle.cloneStyleFrom(guideCellStyle);
        errorCellStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        errorCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle referenceHeaderStyle = workbook.createCellStyle();
        referenceHeaderStyle.cloneStyleFrom(guideHeaderStyle);

        CellStyle referenceCellStyle = workbook.createCellStyle();
        referenceCellStyle.cloneStyleFrom(guideCellStyle);

        return new WorkbookStyles(
                requiredHeaderStyle,
                optionalHeaderStyle,
                guideHeaderStyle,
                oddRowTextStyle,
                evenRowTextStyle,
                oddRowAmountStyle,
                evenRowAmountStyle,
                oddRowDateStyle,
                evenRowDateStyle,
                oddRowDateTimeStyle,
                evenRowDateTimeStyle,
                guideCellStyle,
                errorHeaderStyle,
                errorCellStyle,
                referenceHeaderStyle,
                referenceCellStyle,
                boldFont
        );
    }

    public void writeHeaderRow(Sheet sheet, List<ExcelTemplateColumn> columns, WorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(26);
        CreationHelper creationHelper = sheet.getWorkbook().getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();

        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            ExcelTemplateColumn column = columns.get(columnIndex);
            Cell cell = headerRow.createCell(columnIndex, CellType.STRING);
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(column.required() ? styles.requiredHeaderStyle() : styles.optionalHeaderStyle());

            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(columnIndex);
            anchor.setCol2(columnIndex + 3);
            anchor.setRow1(0);
            anchor.setRow2(4);
            Comment comment = drawing.createCellComment(anchor);
            String commentText = "Mã trường: " + column.fieldKey()
                    + "\nHiển thị: " + (column.hidden() ? "Ẩn - cột kỹ thuật" : "Hiện")
                    + "\nBắt buộc: " + (column.required() ? "Có" : "Không")
                    + "\nĐược import: " + (column.importable() ? "Có" : "Không")
                    + "\nKiểu dữ liệu: " + column.dataType()
                    + "\nVí dụ: " + column.example()
                    + "\nQuy tắc: " + column.rules();
            comment.setString(creationHelper.createRichTextString(commentText));
            cell.setCellComment(comment);
            sheet.setColumnHidden(columnIndex, column.hidden());
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columns.size() - 1));
    }

    public void writeGuideSheet(Sheet sheet, List<ExcelTemplateColumn> columns, WorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        List<String> headers = List.of("Mã trường", "Cột Excel", "Hiển thị", "Bắt buộc", "Được import", "Kiểu dữ liệu", "Ví dụ hợp lệ", "Quy tắc nhập liệu");
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i, CellType.STRING);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(styles.guideHeaderStyle());
        }

        int rowIndex = 1;
        for (ExcelTemplateColumn column : columns) {
            Row row = sheet.createRow(rowIndex++);
            writeTextCell(row, 0, column.fieldKey(), styles.guideCellStyle());
            writeTextCell(row, 1, column.headerLabel(), styles.guideCellStyle());
            writeTextCell(row, 2, column.hidden() ? "Ẩn" : "Hiện", styles.guideCellStyle());
            writeTextCell(row, 3, column.required() ? "Có" : "Không", styles.guideCellStyle());
            writeTextCell(row, 4, column.importable() ? "Có" : "Không", styles.guideCellStyle());
            writeTextCell(row, 5, column.dataType(), styles.guideCellStyle());
            writeTextCell(row, 6, column.example(), styles.guideCellStyle());
            writeTextCell(row, 7, column.rules(), styles.guideCellStyle());
        }

        sheet.createFreezePane(0, 1);
    }

    public void writeReferenceSheet(Sheet sheet, List<ExcelReferenceItem> items, WorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        List<String> headers = List.of("Nhóm tham chiếu", "Giá trị", "Ghi chú");
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i, CellType.STRING);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(styles.referenceHeaderStyle());
        }

        int rowIndex = 1;
        for (ExcelReferenceItem item : items) {
            Row row = sheet.createRow(rowIndex++);
            writeTextCell(row, 0, item.group(), styles.referenceCellStyle());
            writeTextCell(row, 1, item.value(), styles.referenceCellStyle());
            writeTextCell(row, 2, item.note(), styles.referenceCellStyle());
        }

        sheet.createFreezePane(0, 1);
    }

    public void applyDropdownValidation(Sheet sheet, int columnIndex, List<String> values, int startRow, int endRow) {
        if (values == null || values.isEmpty()) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values.toArray(String[]::new));
        CellRangeAddressList addressList = new CellRangeAddressList(startRow, endRow, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    public void autosizeSheet(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 256 * 48));
        }
    }

    public void writeTextCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex, CellType.STRING);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    public void writeLongCell(Row row, int columnIndex, Number value, CellStyle style) {
        if (value == null) {
            writeTextCell(row, columnIndex, "", style);
            return;
        }
        Cell cell = row.createCell(columnIndex, CellType.NUMERIC);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    public void writeDecimalCell(Row row, int columnIndex, BigDecimal value, CellStyle style) {
        if (value == null) {
            writeTextCell(row, columnIndex, "", style);
            return;
        }
        Cell cell = row.createCell(columnIndex, CellType.NUMERIC);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    public void writeDateCell(Row row, int columnIndex, LocalDate value, CellStyle style) {
        if (value == null) {
            writeTextCell(row, columnIndex, "", style);
            return;
        }
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        cell.setCellStyle(style);
    }

    public void writeDateTimeCell(Row row, int columnIndex, LocalDateTime value, CellStyle style) {
        if (value == null) {
            writeTextCell(row, columnIndex, "", style);
            return;
        }
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(Date.from(value.atZone(ZoneId.systemDefault()).toInstant()));
        cell.setCellStyle(style);
    }

    public byte[] toByteArray(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void setAllBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderRight(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    public record WorkbookStyles(
            CellStyle requiredHeaderStyle,
            CellStyle optionalHeaderStyle,
            CellStyle guideHeaderStyle,
            CellStyle oddRowTextStyle,
            CellStyle evenRowTextStyle,
            CellStyle oddRowAmountStyle,
            CellStyle evenRowAmountStyle,
            CellStyle oddRowDateStyle,
            CellStyle evenRowDateStyle,
            CellStyle oddRowDateTimeStyle,
            CellStyle evenRowDateTimeStyle,
            CellStyle guideCellStyle,
            CellStyle errorHeaderStyle,
            CellStyle errorCellStyle,
            CellStyle referenceHeaderStyle,
            CellStyle referenceCellStyle,
            Font boldFont
    ) {
        public CellStyle textRowStyle(int rowIndex) {
            return rowIndex % 2 == 0 ? evenRowTextStyle : oddRowTextStyle;
        }

        public CellStyle amountRowStyle(int rowIndex) {
            return rowIndex % 2 == 0 ? evenRowAmountStyle : oddRowAmountStyle;
        }

        public CellStyle dateRowStyle(int rowIndex) {
            return rowIndex % 2 == 0 ? evenRowDateStyle : oddRowDateStyle;
        }

        public CellStyle dateTimeRowStyle(int rowIndex) {
            return rowIndex % 2 == 0 ? evenRowDateTimeStyle : oddRowDateTimeStyle;
        }
    }
}
