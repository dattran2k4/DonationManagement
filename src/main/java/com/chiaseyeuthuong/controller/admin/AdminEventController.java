package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.service.CategoryService;
import com.chiaseyeuthuong.service.EventService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;
    private final CategoryService categoryService;

    @GetMapping
    public String showAdminEventPage(Model model) {
        model.addAttribute("totalEvents", eventService.getEventCount(null));
        model.addAttribute("totalUpcomingEvents", eventService.getEventCount(EEventStatus.UPCOMING));
        model.addAttribute("totalOngoingEvents", eventService.getEventCount(EEventStatus.ONGOING));

        return "/pages/admin/events";
    }

    @GetMapping("/form")
    public String showCreatEventPage(Model model) {
        model.addAttribute("event", new EventRequest());
        model.addAttribute("eventFormId", null);
        model.addAttribute("eventActivities", List.of());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/pages/admin/event-form";
    }

    @GetMapping("/{id}/form")
    public String showEditEventPage(@Min(1) @PathVariable Long id, Model model) {
        var event = eventService.getEventById(id);
        model.addAttribute("event", event);
        model.addAttribute("eventFormId", event.getId());
        model.addAttribute("eventActivities", event.getActivities());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/pages/admin/event-form";
    }
}
