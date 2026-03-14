package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.TransactionResponse;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Transaction;
import vn.payos.model.webhooks.WebhookData;

public interface TransactionService {
    PageResponse<TransactionResponse> getTransactions(int page, int size, String search, EPaymentMethod method);

    void createTransactionFromPayOS(WebhookData data, Donation donation);

    TransactionResponse getTransactionById(Long id);
}
