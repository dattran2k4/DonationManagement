package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.common.ETransactionStatus;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.TransactionResponse;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Transaction;
import com.chiaseyeuthuong.repository.TransactionRepository;
import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TRANSACTION-SERVICE")
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public PageResponse<TransactionResponse> getTransactions(int page, int size, ETransactionStatus status) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        PageRequest pageRequest = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Transaction> transactionPage = transactionRepository.findAll(pageRequest);

        List<TransactionResponse> response = transactionPage.stream().map(this::toResponse).toList();

        return PageResponse.<TransactionResponse>builder()
                .page(page)
                .pageSize(size)
                .totalItems(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .data(response)
                .build();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        BeanUtils.copyProperties(transaction, response);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTransactionFromPayOS(WebhookData data, Donation donation) {
        log.info("Processing create transaction from PayOS");

        if (transactionRepository.existsByTransactionCode(data.getReference())) {
            log.info("Transaction with code {} already exists", data.getReference());
            return;
        }

        Transaction transaction = new Transaction();

        transaction.setTransactionCode(data.getReference());
        transaction.setPaymentMethod(EPaymentMethod.BANK_TRANSFER_ONLINE);
        transaction.setAccountBankId(data.getCounterAccountBankId());
        transaction.setTransactionDateTime(data.getTransactionDateTime());
        transaction.setAmount(BigDecimal.valueOf(data.getAmount()));
        transaction.setDescription(data.getDescription());
        transaction.setCounterAccountName(data.getCounterAccountName());
        transaction.setCounterAccountNumber(data.getCounterAccountNumber());
        transaction.setRawApiData(data.toString());
        transaction.setDonation(donation);

        Transaction result = transactionRepository.save(transaction);
        log.info("Transaction created successfully with id {}", result.getId());
    }

    @Override
    public TransactionResponse getTransactionById(Long id) {
        return toResponse(transactionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction not found")));
    }
}
