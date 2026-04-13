package com.chiaseyeuthuong.excel;

import java.util.List;

public record ExcelTemplateColumn(
        String fieldKey,
        String headerLabel,
        boolean required,
        boolean importable,
        String dataType,
        String example,
        String rules,
        List<String> dropdownValues,
        boolean hidden
) {
    public ExcelTemplateColumn(String fieldKey,
                               String headerLabel,
                               boolean required,
                               boolean importable,
                               String dataType,
                               String example,
                               String rules,
                               List<String> dropdownValues) {
        this(fieldKey, headerLabel, required, importable, dataType, example, rules, dropdownValues, false);
    }
}
