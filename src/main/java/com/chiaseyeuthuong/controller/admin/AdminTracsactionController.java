package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/transactions")
public class AdminTracsactionController {

    private final TransactionService transactionService;

    @GetMapping
    public String showAdminTransactionPage(Model model) {
        return "pages/admin/transactions";
    }

    @GetMapping("/{id}")
    public String showAdminTransactionDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("transaction", transactionService.getTransactionById(id));
        return "pages/admin/transaction-detail";
    }
}
