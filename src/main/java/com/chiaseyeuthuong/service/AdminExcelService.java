package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.response.ExcelImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface AdminExcelService {

    byte[] exportEvents(String search, EEventStatus status, String sortBy, String sortDir, String... categoryIds);

    ExcelImportResult importEvents(MultipartFile file);

    byte[] exportActivities(String search, EActivityStatus status);

    ExcelImportResult importActivities(MultipartFile file);

    byte[] exportDonors(String search, EDonorType type, String sortBy, String sortDir);

    ExcelImportResult importDonors(MultipartFile file);

    byte[] exportDonations(String search, EDonationStatus status, EDonationTarget target, EDonationType type,
                           EPaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount);

    ExcelImportResult importDonations(MultipartFile file, String username);

    byte[] exportTransactions(String search, EPaymentMethod method);

    ExcelImportResult importTransactions(MultipartFile file);
}
