package com.chiaseyeuthuong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditChangeItemResponse {
    private String field;
    private String oldValue;
    private String newValue;
}
