package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.common.ETransactionStatus;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-TRANSACTION-CONTROLLER")
@RequestMapping("/api/transactions")
public class ApiTransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ApiResponse getAllTransactions(@RequestParam(required = false, defaultValue = "0") int page,
                                          @RequestParam(required = false, defaultValue = "10") int size,
                                          @RequestParam(required = false) ETransactionStatus status,
                                          @RequestParam(required = false) EPaymentMethod method) {
        return ApiResponse.builder()
                .status(200)
                .message("Transactions found!")
                .data(transactionService.getTransactions(page, size, status))
                .build();
    }
}
