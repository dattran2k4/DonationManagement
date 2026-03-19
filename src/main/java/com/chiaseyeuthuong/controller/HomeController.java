package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeController {

    private final DonorService donorService;
    private final EventService eventService;
    private final ActivityService activityService;

    @GetMapping
    public String showHomePage(Model model) {
        model.addAttribute("totalDonors", donorService.getDorCountByObjectId(null, null));
        model.addAttribute("totalEvents", eventService.getEventCount(null));
        model.addAttribute("totalActivities", activityService.getActivityCount());
        model.addAttribute("ongoingEvents", eventService
                .getAllEvents(1, 3, "id", "desc", null, EEventStatus.ONGOING, true)
                .getData());
        return "pages/web/index";
    }
}
