package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.service.CategoryService;
import com.chiaseyeuthuong.service.EventService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;
    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminEventPage(Model model) {
        model.addAttribute("totalEvents", eventService.getEventCount(null));
        model.addAttribute("totalUpcomingEvents", eventService.getEventCount(EEventStatus.UPCOMING));
        model.addAttribute("totalOngoingEvents", eventService.getEventCount(EEventStatus.ONGOING));

        return "/pages/admin/events";
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public String showCreateEventPage(Model model) {
        model.addAttribute("event", new EventResponse());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/pages/admin/event-detail";
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public String showEditEventPage(@Min(1) @PathVariable Long id) {
        return "redirect:/admin/events/" + id;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showEventDetailPage(@Min(1) @PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEventById(id));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/pages/admin/event-detail";
    }
}
