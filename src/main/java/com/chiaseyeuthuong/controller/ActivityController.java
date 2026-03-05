package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/activities/{slug}")
    public String showActivityDetailPage(@PathVariable String slug, Model model) {

        model.addAttribute("activity", activityService.getActivityBySlug(slug));
        return "pages/web/activity-detail";
    }

}
