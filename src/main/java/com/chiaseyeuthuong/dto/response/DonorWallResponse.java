package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EDonorWallPeriod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class DonorWallResponse {
    private EDonorWallPeriod period;
    private String periodLabel;
    private Integer year;
    private Integer month;
    private Integer quarter;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<DonorWallItemResponse> donors;
}
