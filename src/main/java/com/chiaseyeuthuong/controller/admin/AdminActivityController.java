package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.dto.response.ActivityResponse;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/activities")
public class AdminActivityController {

    private final ActivityService activityService;
    private final EventService eventService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminActivityPage(Model model) {
        return "pages/admin/activities";
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public String showAdminActivityCreateFormPage(@RequestParam(required = false) Long eventId, Model model) {
        ActivityResponse activityResponse = new ActivityResponse();
        if (eventId != null) {
            var event = eventService.getEventById(eventId);
            activityResponse.setEventId(eventId);
            activityResponse.setEvent(event);
            activityResponse.setStartDate(event.getStartDate());
        }

        model.addAttribute("activity", activityResponse);
        model.addAttribute("statuses", EActivityStatus.values());
        model.addAttribute("events", eventService.getAllEvents(0, 9999, null, null, null, null, false, (String[]) null));
        return "pages/admin/activity-detail";
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public String showAdminActivityEditFormPage(@PathVariable Long id, Model model) {
        return "redirect:/admin/activities/" + id;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminActivityDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("activity", activityService.getActivityById(id));
        model.addAttribute("statuses", EActivityStatus.values());
        model.addAttribute("events", eventService.getAllEvents(0, 9999, null, null, null, null, false, (String[]) null));
        return "pages/admin/activity-detail";
    }
}
