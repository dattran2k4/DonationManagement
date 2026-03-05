package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.ETransactionStatus;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.TransactionResponse;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Transaction;
import vn.payos.model.webhooks.WebhookData;

public interface TransactionService {
    PageResponse<TransactionResponse> getTransactions(int page, int size, ETransactionStatus status);

    void createTransactionFromPayOS(WebhookData data, Donation donation);

    TransactionResponse getTransactionById(Long id);
}