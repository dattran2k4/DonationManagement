package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.service.DonorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/donors")
public class AdminDonorController {

    private final DonorService donorService;

    @GetMapping
    public String showDonorsPage(Model model) {
        return "pages/admin/donors";
    }

    @GetMapping("/form")
    public String showCreateDonorPage(Model model) {
        return "pages/admin/donor-form";
    }

    @GetMapping("/{id}/form")
    public String showEditDonorPage(@PathVariable Long id, Model model) {
        return "pages/admin/donor-form";
    }
}
