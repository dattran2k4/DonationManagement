package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.dto.response.AdminDashboardSummaryResponse;
import com.chiaseyeuthuong.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/dashboard")
public class AdminDashBoardController {

    private final DashboardService dashboardService;

    @GetMapping
    public String showAdminDashBoardPage(Model model) {
        AdminDashboardSummaryResponse summary = dashboardService.getAdminDashboardSummary();
        model.addAttribute("dashboardSummary", summary);
        return "pages/admin/dashboard";
    }

}
