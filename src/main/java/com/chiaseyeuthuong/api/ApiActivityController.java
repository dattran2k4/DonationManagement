package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.dto.request.ActivityRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-ACTIVITY-CONTROLLER")
@RequestMapping("/api/activities")
public class ApiActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ApiResponse getAllActivities(@RequestParam(required = false, defaultValue = "0") int page,
                                        @RequestParam(required = false, defaultValue = "20") int size, String search, EActivityStatus status) {
        return ApiResponse.builder()
                .status(200)
                .message("OK")
                .data(activityService.getAllActivities(page, size, search, status))
                .build();
    }

    @GetMapping("/view-activities")
    public ApiResponse getAllActivitiesByEventId(@RequestParam Long eventId) {
        return ApiResponse.builder()
                .status(200)
                .message("OK")
                .data(activityService.getAllActivitiesByEventId(eventId))
                .build();
    }

    @GetMapping("/{id}/detail")
    public ApiResponse getActivityById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("OK")
                .data(activityService.getActivityById(id))
                .build();
    }

    @PostMapping("/save")
    public ApiResponse saveActivity(@RequestBody @Valid ActivityRequest request) {
        activityService.saveActivity(request);
        return ApiResponse.builder()
                .status(200)
                .message("Successfully saved activity")
                .build();
    }

    @PostMapping({"/upload", "/{id}/upload"})
    public ApiResponse uploadThumbnail(@PathVariable(required = false) Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.builder()
                .status(201)
                .message("Successfully updated thumbnail event")
                .data(activityService.saveThumbnailUrl(id, file))
                .build();
    }
}
