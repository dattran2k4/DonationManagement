package com.chiaseyeuthuong.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationResponse {

    private String taxCode;

    private String name;

    private String representative;

    private String billingAddress;
}
