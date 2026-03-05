package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/activities")
    public String showActivitiesPage(Model model) {
        return "pages/web/activities";
    }

    @GetMapping("/events/activities")
    public String showActiviesByEventPage(Model model) {
        return "pages/web/activities-by-event";
    }

}
