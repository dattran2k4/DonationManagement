package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/activities")
public class AdminActivityController {

    private final ActivityService activityService;
    private final EventService eventService;

    @GetMapping
    public String showAdminActivityPage(Model model) {
        return "pages/admin/activities";
    }

    @GetMapping("/form")
    public String showAdminActivityCreateFormPage(Model model) {
        model.addAttribute("activity", new ActivityRequest());
        model.addAttribute("statuses", EActivityStatus.values());
        model.addAttribute("events", eventService.getAllEvents(0, 9999, null, null, null, null, null));
        return "pages/admin/activity-form";
    }

    @GetMapping("/{id}/form")
    public String showAdminActivityEditFormPage(@PathVariable Long id, Model model) {
        model.addAttribute("activity", activityService.getActivityById(id));
        model.addAttribute("statuses", EActivityStatus.values());
        model.addAttribute("events", eventService.getAllEvents(0, 9999, null, null, null, null, null));
        return "pages/admin/activity-form";
    }
}
