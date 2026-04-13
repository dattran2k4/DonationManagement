package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.response.ExcelImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface AdminExcelService {

    byte[] exportEvents(String search, EEventStatus status, String sortBy, String sortDir, String... categoryIds);

    byte[] downloadEventsTemplate();

    ExcelImportResult importEvents(MultipartFile file);

    byte[] exportActivities(String search, EActivityStatus status, String sortBy, String sortDir);

    byte[] downloadActivitiesTemplate();

    ExcelImportResult importActivities(MultipartFile file);

    byte[] exportDonors(String search, EDonorType type, String sortBy, String sortDir);

    byte[] downloadDonorsTemplate();

    ExcelImportResult importDonors(MultipartFile file);

    byte[] exportDonations(String search, EDonationStatus status, EDonationTarget target, EDonationType type,
                           EPaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount,
                           String sortBy, String sortDir);

    byte[] downloadDonationsTemplate();

    ExcelImportResult importDonations(MultipartFile file, String username);

    byte[] exportTransactions(String search, EPaymentMethod method, String sortBy, String sortDir);

    byte[] downloadTransactionsTemplate();

    ExcelImportResult importTransactions(MultipartFile file);
}
