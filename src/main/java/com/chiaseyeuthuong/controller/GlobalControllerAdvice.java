package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.service.CategoryService;
import com.chiaseyeuthuong.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CategoryService categoryService;
    private final SystemConfigService systemConfigService;

    @ModelAttribute
    public void addCommonData(Model model) {
        model.addAttribute("hello", "world");
        model.addAttribute("statuses", EEventStatus.values());
        model.addAttribute("publicEventStatuses", new EEventStatus[]{
                EEventStatus.UPCOMING,
                EEventStatus.ONGOING,
                EEventStatus.COMPLETED
        });
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("systemConfig", systemConfigService.getAllSystemConfigMap());
    }
}
