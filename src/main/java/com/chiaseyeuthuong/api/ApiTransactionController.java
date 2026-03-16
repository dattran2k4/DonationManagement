package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse getAllTransactions(@RequestParam(required = false, defaultValue = "1") int page,
                                          @RequestParam(required = false, defaultValue = "10") int size,
                                          @RequestParam(required = false) String search,
                                          @RequestParam(required = false) EPaymentMethod method) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách giao dịch thành công")
                .data(transactionService.getTransactions(page, size, search, method))
                .build();
    }
}
