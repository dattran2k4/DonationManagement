package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.model.Category;
import com.chiaseyeuthuong.model.Event;
import com.chiaseyeuthuong.service.CategoryService;
import com.chiaseyeuthuong.service.EventService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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
    public String showAdminEventPage(Model model) {
        model.addAttribute("totalEvents", eventService.getEventCount(null));
        model.addAttribute("totalUpcomingEvents", eventService.getEventCount(EEventStatus.UPCOMING));
        model.addAttribute("totalOngoingEvents", eventService.getEventCount(EEventStatus.ONGOING));

        return "/pages/admin/events";
    }

    @GetMapping("/form")
    public String showCreatEventPage(Model model) {
        model.addAttribute("event", new EventRequest());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/pages/admin/event-form";
    }

    @GetMapping("/{id}/form")
    public String showEditEventPage(@Min(1) @PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEventById(id));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/pages/admin/event-form";
    }
}
