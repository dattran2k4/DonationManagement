package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.request.EventRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-EVENT-CONTROLLER")
@RequestMapping("/api/events")
public class ApiEventController {

    private final EventService eventService;
    private final ActivityService activityService;

    @GetMapping
    public ApiResponse getAllEvents(@RequestParam(required = false, defaultValue = "0") int page,
                                    @RequestParam(required = false, defaultValue = "10") int size,
                                    @RequestParam(required = false) String sortBy,
                                    @RequestParam(required = false) String sortDir,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) EEventStatus status,
                                    @RequestParam(required = false) String... categoryIds) {
        return ApiResponse.builder()
                .status(200)
                .message("List events")
                .data(eventService.getAllEvents(page, size, sortBy, sortDir, search, status, categoryIds))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse getEventById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Get event by id")
                .data(eventService.getEventById(id))
                .build();
    }

    @GetMapping("/{id}/activities")
    public ApiResponse getAllActivitiesByEventId(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Get activities by event id")
                .data(activityService.getAllActivitiesByEventId(id))
                .build();
    }

    @PostMapping("/save")
    public ApiResponse saveEvent(@RequestBody @Valid EventRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Successfully saved event")
                .data(eventService.saveEvent(request))
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse updateStatus(@Min(1) @PathVariable Long id, @RequestParam EEventStatus status) {
        eventService.updateStatus(status, id);
        return ApiResponse.builder()
                .status(200)
                .message("Successfully updated status event")
                .build();
    }

    @PostMapping({"/upload", "/{id}/upload"})
    public ApiResponse uploadThumbnail(@PathVariable(required = false) Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.builder()
                .status(201)
                .message("Successfully updated thumbnail event")
                .data(eventService.saveThumbnailUrl(id, file))
                .build();
    }
}
