package com.chiaseyeuthuong.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigResponse {

    private Long id;
    private String key;
    private String value;
    private String description;
}
