package com.chiaseyeuthuong.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingController {

    @GetMapping
    public String showAdminSettingsPage(Model model) {
        return "pages/admin/settings";
    }
}
