package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.common.sort.SortParamUtils;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.TransactionResponse;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Transaction;
import com.chiaseyeuthuong.repository.TransactionRepository;
import com.chiaseyeuthuong.service.TransactionService;
import com.chiaseyeuthuong.service.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TRANSACTION-SERVICE")
public class TransactionServiceImpl implements TransactionService {

    private static final Map<String, String> TRANSACTION_SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("code", "transactionCode"),
            Map.entry("transactionCode", "transactionCode"),
            Map.entry("amount", "amount"),
            Map.entry("counterAccountName", "counterAccountName"),
            Map.entry("counterAccountNumber", "counterAccountNumber"),
            Map.entry("donationCode", "donation.memoCode"),
            Map.entry("paymentMethod", "paymentMethod"),
            Map.entry("transactionDateTime", "transactionDateTime"),
            Map.entry("createdAt", "createdAt")
    );

    private final TransactionRepository transactionRepository;

    @Override
    public PageResponse<TransactionResponse> getTransactions(int page, int size, String search, EPaymentMethod method,
                                                             String sortBy, String sortDir) {
        Sort sort = SortParamUtils.buildSort(TRANSACTION_SORT_FIELDS, Map.of(),
                sortBy, sortDir, "transactionDateTime", Sort.Direction.DESC, "id");

        Specification<Transaction> specification = TransactionSpecification.filterTransaction(search, method);
        Page<Transaction> transactionPage = transactionRepository.findAll(specification,
                SortParamUtils.buildPageRequest(page, size, sort, 10));

        List<TransactionResponse> response = transactionPage.stream().map(this::toResponse).toList();

        return PageResponse.<TransactionResponse>builder()
                .page(SortParamUtils.normalizePageNumber(page) + 1)
                .pageSize(SortParamUtils.normalizePageSize(size, 10))
                .totalItems(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .data(response)
                .build();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        BeanUtils.copyProperties(transaction, response);
        if (transaction.getDonation() != null) {
            response.setDonationId(transaction.getDonation().getId());
            response.setDonationCode(transaction.getDonation().getMemoCode());
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTransactionFromPayOS(WebhookData data, Donation donation) {
        log.info("Processing create transaction from PayOS");

        Transaction transaction = new Transaction();

        transaction.setAmount(donation.getAmount());
        transaction.setPaymentMethod(
                donation.getPaymentMethod() != null
                        ? donation.getPaymentMethod()
                        : EPaymentMethod.BANK_TRANSFER_ONLINE
        );

        if (data != null) {

            if (transactionRepository.existsByTransactionCode(data.getReference())) {
                log.info("Transaction with code {} already exists", data.getReference());
                return;
            }

            transaction.setTransactionCode(data.getReference());
            transaction.setAccountBankId(data.getCounterAccountBankId());
            transaction.setTransactionDateTime(data.getTransactionDateTime());
            transaction.setAmount(BigDecimal.valueOf(data.getAmount()));
            transaction.setDescription(data.getDescription());
            transaction.setCounterAccountName(data.getCounterAccountName());
            transaction.setCounterAccountNumber(data.getCounterAccountNumber());
            transaction.setRawApiData(data.toString());
            transaction.setPaymentMethod(EPaymentMethod.BANK_TRANSFER_ONLINE);
        }

        transaction.setDonation(donation);

        Transaction result = transactionRepository.save(transaction);
        log.info("Transaction created successfully with id {}", result.getId());
    }

    @Override
    public TransactionResponse getTransactionById(Long id) {
        return toResponse(transactionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction not found")));
    }
}
