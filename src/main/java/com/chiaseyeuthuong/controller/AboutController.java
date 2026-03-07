package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/about")
public class AboutController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public String showAboutPage(Model model) {
        return "pages/web/about-us";
    }
}
