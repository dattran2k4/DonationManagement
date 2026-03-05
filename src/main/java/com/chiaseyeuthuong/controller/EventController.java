package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.dto.response.EventResponse;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public String showEventPage(Model model) {
        return "pages/web/events";
    }

    @GetMapping("/{slug}")
    public String showEventDetailPage(@PathVariable("slug") String slug, Model model) {
        model.addAttribute("event", eventService.getEventBySlug(slug));
        return "pages/web/event-detail";
    }

}
